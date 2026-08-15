package dev.thefoolish.aidao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-controlled GitHub transport for generated Android projects.
 *
 * Generated source is written only to an isolated generated branch. The trusted
 * build workflow remains on the repository default branch and is triggered via
 * repository_dispatch, avoiding a requirement to write workflow files from the
 * Android client.
 */
final class GitHubGeneratedBuildClient {
    static final class BuildReceipt {
        final String branch;
        final long runId;
        final String runUrl;
        final String artifactName;
        final String artifactUrl;
        final String conclusion;
        final String failureSummary;

        BuildReceipt(String branch,long runId,String runUrl,String artifactName,String artifactUrl,String conclusion,String failureSummary){
            this.branch=branch;
            this.runId=runId;
            this.runUrl=runUrl;
            this.artifactName=artifactName;
            this.artifactUrl=artifactUrl;
            this.conclusion=conclusion;
            this.failureSummary=failureSummary;
        }

        boolean success(){return "success".equalsIgnoreCase(conclusion)&&artifactName!=null;}
    }

    interface Progress { void onProgress(String stage,String detail); }

    BuildReceipt sendBuildAndWait(String repoFullName,String token,GeneratedProject project,Progress progress) throws Exception {
        requireRepo(repoFullName);
        if(token==null||token.trim().isEmpty()) throw new IllegalArgumentException("GitHub token is required for this explicit send/build action.");

        String[] parts=repoFullName.trim().split("/");
        String owner=parts[0],repo=parts[1];

        progress.onProgress("GitHub preflight","Checking repository access and default branch.");
        String repoJson=get("https://api.github.com/repos/"+owner+"/"+repo,token);
        String defaultBranch=value(repoJson,"default_branch");
        if(defaultBranch==null) defaultBranch="main";

        String refJson=get("https://api.github.com/repos/"+owner+"/"+repo+"/git/ref/heads/"+url(defaultBranch),token);
        String parentSha=nestedSha(refJson);
        if(parentSha==null) throw new IllegalStateException("Could not resolve the repository default branch commit.");

        List<GeneratedProject.FileEntry> sourceFiles=new ArrayList<>();
        for(GeneratedProject.FileEntry f:project.files){
            if(f.path.startsWith(".github/workflows/")) continue;
            sourceFiles.add(f);
        }
        if(sourceFiles.isEmpty()) throw new IllegalStateException("Generated project contains no source files to upload.");

        progress.onProgress("Uploading generated source",sourceFiles.size()+" files to a new isolated branch.");
        List<String> blobShas=new ArrayList<>();
        for(GeneratedProject.FileEntry f:sourceFiles){
            String body="{\"content\":\""+json(f.content)+"\",\"encoding\":\"utf-8\"}";
            String r=post("https://api.github.com/repos/"+owner+"/"+repo+"/git/blobs",token,body);
            String sha=value(r,"sha");
            if(sha==null) throw new IllegalStateException("GitHub did not return a blob SHA for "+f.path);
            blobShas.add(sha);
        }

        StringBuilder tree=new StringBuilder("{\"tree\":[");
        for(int i=0;i<sourceFiles.size();i++){
            if(i>0) tree.append(',');
            tree.append("{\"path\":\"")
                .append(json(sourceFiles.get(i).path))
                .append("\",\"mode\":\"100644\",\"type\":\"blob\",\"sha\":\"")
                .append(blobShas.get(i)).append("\"}");
        }
        tree.append("]}");

        String treeJson=post("https://api.github.com/repos/"+owner+"/"+repo+"/git/trees",token,tree.toString());
        String treeSha=value(treeJson,"sha");
        if(treeSha==null) throw new IllegalStateException("GitHub did not create the generated source tree.");

        String commitBody="{\"message\":\"AIDao generated Android project: "+json(project.projectName)+"\",\"tree\":\""+treeSha+"\",\"parents\":[\""+parentSha+"\"]}";
        String commitJson=post("https://api.github.com/repos/"+owner+"/"+repo+"/git/commits",token,commitBody);
        String commitSha=value(commitJson,"sha");
        if(commitSha==null) throw new IllegalStateException("GitHub did not create the generated project commit.");

        String branch="aidao-generated-"+slug(project.projectName)+"-"+(System.currentTimeMillis()/1000L);
        post("https://api.github.com/repos/"+owner+"/"+repo+"/git/refs",token,"{\"ref\":\"refs/heads/"+branch+"\",\"sha\":\""+commitSha+"\"}");

        progress.onProgress("Starting Android CI","Generated branch created. Triggering trusted repository workflow.");
        String dispatch="{\"event_type\":\"aidao-generated-build\",\"client_payload\":{\"target_branch\":\""+json(branch)+"\",\"project_name\":\""+json(project.projectName)+"\"}}";
        postNoContent("https://api.github.com/repos/"+owner+"/"+repo+"/dispatches",token,dispatch);

        long runId=0;
        String runUrl=null,conclusion=null;
        long deadline=System.currentTimeMillis()+12*60*1000L;
        while(System.currentTimeMillis()<deadline){
            Thread.sleep(6000);
            String runs=get("https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/generated-project.yml/runs?event=repository_dispatch&per_page=5",token);
            String title=firstValue(runs,"display_title");
            if(title!=null&&!title.contains(branch)) continue;
            runId=firstLong(runs,"id");
            runUrl=firstValue(runs,"html_url");
            String status=firstValue(runs,"status");
            conclusion=firstValue(runs,"conclusion");
            if(runId>0) progress.onProgress("Android CI",status==null?"Workflow discovered":"Workflow "+status+(conclusion==null?"":" · "+conclusion));
            if(runId>0&&"completed".equalsIgnoreCase(status)) break;
        }

        if(runId==0) throw new IllegalStateException("Generated branch was created, but no trusted Android CI run appeared. Confirm Actions are enabled for the repository.");
        if(!"success".equalsIgnoreCase(conclusion)){
            String failure=fetchFailureSummary(owner,repo,token,runId);
            return new BuildReceipt(branch,runId,runUrl,null,null,conclusion,failure);
        }

        String artifacts=get("https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs/"+runId+"/artifacts?per_page=20",token);
        String artifactName=firstValue(artifacts,"name");
        String artifactApi=firstValue(artifacts,"archive_download_url");
        if(artifactName==null) throw new IllegalStateException("CI succeeded but no generated APK artifact was uploaded.");

        progress.onProgress("APK ready","CI succeeded and uploaded "+artifactName+".");
        return new BuildReceipt(branch,runId,runUrl,artifactName,artifactApi,"success",null);
    }

    private String fetchFailureSummary(String owner,String repo,String token,long runId){
        try{
            String jobs=get("https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs/"+runId+"/jobs?per_page=20",token);
            long jobId=firstLong(jobs,"id");
            String jobName=firstValue(jobs,"name");
            String conclusion=firstValue(jobs,"conclusion");
            return "CI failure in "+(jobName==null?"build":jobName)+(conclusion==null?"":" ("+conclusion+")")+". Run ID "+runId+", job ID "+jobId+".";
        }catch(Exception e){
            return "CI failed. Run ID "+runId+". Detailed job lookup was unavailable: "+e.getMessage();
        }
    }

    static String boundedRepairHint(String failure){
        if(failure==null) return "Retry only after inspecting the failed CI run.";
        String f=failure.toLowerCase();
        if(f.contains("gradle")) return "Bounded repair: regenerate Gradle settings only; do not alter product requirements.";
        if(f.contains("manifest")) return "Bounded repair: correct generated AndroidManifest.xml declarations only.";
        if(f.contains("resource")||f.contains("style")) return "Bounded repair: repair generated Android resources/styles only.";
        if(f.contains("compile")) return "Bounded repair: repair generated source compile errors without changing repository/default-branch state.";
        return "Bounded repair: regenerate the deterministic source tree and retry once; preserve the plan and user-controlled GitHub boundary.";
    }

    private void requireRepo(String v){
        if(v==null||!v.trim().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Repository must use owner/name format.");
    }

    private String get(String u,String token)throws Exception{return request("GET",u,token,null,false);}
    private String post(String u,String token,String body)throws Exception{return request("POST",u,token,body,false);}
    private void postNoContent(String u,String token,String body)throws Exception{request("POST",u,token,body,true);}

    private String request(String method,String u,String token,String body,boolean allowEmpty)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(12000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
        c.setRequestProperty("User-Agent","AIDao-Android");
        c.setRequestProperty("Authorization","Bearer "+token.trim());
        if(body!=null){
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type","application/json; charset=utf-8");
            try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
        }

        int code=c.getResponseCode();
        java.io.InputStream stream=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();
        StringBuilder b=new StringBuilder();
        if(stream!=null){
            BufferedReader r=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
            String line;
            while((line=r.readLine())!=null)b.append(line);
            r.close();
        }
        c.disconnect();

        if(code<200||code>=300){
            String detail=trim(b.toString(),700);
            if(code==401) throw new IllegalStateException("GitHub rejected the token (401). Create a current fine-grained token for this repository.");
            if(code==403) throw new IllegalStateException("GitHub denied this action (403). The token needs repository Contents: read/write and Actions: read access. GitHub detail: "+detail);
            if(code==404) throw new IllegalStateException("GitHub resource not found (404). Check repository selection and token repository access. GitHub detail: "+detail);
            throw new IllegalStateException("GitHub API "+code+": "+detail);
        }
        if(allowEmpty&&b.length()==0) return "{}";
        return b.toString();
    }

    private String value(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);return m.find()?m.group(1):null;}
    private String firstValue(String json,String key){return value(json,key);}
    private long firstLong(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(\\d+)").matcher(json);return m.find()?Long.parseLong(m.group(1)):0L;}
    private String nestedSha(String json){Matcher m=Pattern.compile("\\\"object\\\"\\s*:\\s*\\{[^}]*\\\"sha\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);return m.find()?m.group(1):value(json,"sha");}
    private String json(String s){if(s==null)return "";StringBuilder b=new StringBuilder();for(char ch:s.toCharArray()){switch(ch){case '\\':b.append("\\\\");break;case '"':b.append("\\\"");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(ch<32)b.append(String.format("\\u%04x",(int)ch));else b.append(ch);}}return b.toString();}
    private String slug(String s){String v=(s==null?"app":s.toLowerCase()).replaceAll("[^a-z0-9]+","-").replaceAll("^-|-$","");return v.isEmpty()?"app":trim(v,24);}
    private String url(String s){return s.replace(" ","%20");}
    private String trim(String s,int n){return s==null?"":s.substring(0,Math.min(s.length(),n));}
}
