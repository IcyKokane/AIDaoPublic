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
 *
 * Fine-grained token minimum for the full on-device path:
 * - repository Contents: read/write (branch/source upload + repository_dispatch)
 * - repository Actions: read (workflow/run/job/artifact observation)
 * Metadata read is implicit. Workflows write is intentionally not required.
 */
final class GitHubGeneratedBuildClient {
    static final String TOKEN_PERMISSION_SUMMARY = "Contents: read/write + Actions: read";

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

    static final class PreflightReceipt {
        final String defaultBranch;
        final String parentSha;
        final String workflowName;
        PreflightReceipt(String defaultBranch,String parentSha,String workflowName){
            this.defaultBranch=defaultBranch;
            this.parentSha=parentSha;
            this.workflowName=workflowName;
        }
    }

    private static final class RunMatch {
        long id;
        String url;
        String status;
        String conclusion;
    }

    interface Progress { void onProgress(String stage,String detail); }

    BuildReceipt sendBuildAndWait(String repoFullName,String token,GeneratedProject project,Progress progress) throws Exception {
        requireRepo(repoFullName);
        if(token==null||token.trim().isEmpty()) throw new IllegalArgumentException("GitHub token is required for this explicit send/build action.");
        if(progress==null) progress=(s,d)->{};

        String[] parts=repoFullName.trim().split("/");
        String owner=parts[0],repo=parts[1];
        PreflightReceipt preflight=preflight(owner,repo,token,progress);

        List<GeneratedProject.FileEntry> sourceFiles=new ArrayList<>();
        for(GeneratedProject.FileEntry f:project.files){
            if(f.path.startsWith(".github/workflows/")) continue;
            sourceFiles.add(f);
        }
        if(sourceFiles.isEmpty()) throw new IllegalStateException("SOURCE ERROR: Generated project contains no source files to upload.");

        progress.onProgress("Uploading generated source",sourceFiles.size()+" files to a new isolated generated branch.");
        List<String> blobShas=new ArrayList<>();
        for(GeneratedProject.FileEntry f:sourceFiles){
            String body="{\"content\":\""+json(f.content)+"\",\"encoding\":\"utf-8\"}";
            String r=request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/git/blobs",token,body,false,"source blob "+f.path);
            String sha=value(r,"sha");
            if(sha==null) throw new IllegalStateException("GITHUB SOURCE ERROR: GitHub did not return a blob SHA for "+f.path+".");
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

        String treeJson=request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/git/trees",token,tree.toString(),false,"generated source tree");
        String treeSha=value(treeJson,"sha");
        if(treeSha==null) throw new IllegalStateException("GITHUB SOURCE ERROR: GitHub did not create the generated source tree.");

        String commitBody="{\"message\":\"AIDao generated Android project: "+json(project.projectName)+"\",\"tree\":\""+treeSha+"\",\"parents\":[\""+preflight.parentSha+"\"]}";
        String commitJson=request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/git/commits",token,commitBody,false,"generated project commit");
        String commitSha=value(commitJson,"sha");
        if(commitSha==null) throw new IllegalStateException("GITHUB SOURCE ERROR: GitHub did not create the generated project commit.");

        String branch="aidao-generated-"+slug(project.projectName)+"-"+(System.currentTimeMillis()/1000L);
        request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/git/refs",token,"{\"ref\":\"refs/heads/"+branch+"\",\"sha\":\""+commitSha+"\"}",false,"generated branch "+branch);

        progress.onProgress("Starting Android CI","Generated branch created. Triggering trusted workflow from "+preflight.defaultBranch+".");
        String dispatch="{\"event_type\":\"aidao-generated-build\",\"client_payload\":{\"target_branch\":\""+json(branch)+"\",\"project_name\":\""+json(project.projectName)+"\"}}";
        request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/dispatches",token,dispatch,true,"repository_dispatch");

        long runId=0;
        String runUrl=null,conclusion=null;
        long deadline=System.currentTimeMillis()+12*60*1000L;
        while(System.currentTimeMillis()<deadline){
            Thread.sleep(6000);
            String runs=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/generated-project.yml/runs?event=repository_dispatch&per_page=10",token,null,false,"generated workflow runs");
            RunMatch match=findRunForBranch(runs,branch);
            if(match==null) continue;
            runId=match.id;
            runUrl=match.url;
            conclusion=match.conclusion;
            progress.onProgress("Android CI",match.status==null?"Workflow discovered":"Workflow "+match.status+(conclusion==null?"":" · "+conclusion));
            if(runId>0&&"completed".equalsIgnoreCase(match.status)) break;
        }

        if(runId==0) throw new IllegalStateException("WORKFLOW ERROR: repository_dispatch was accepted, but no matching Generated Project CI run appeared for branch "+branch+" within 12 minutes. Confirm Actions are enabled and generated-project.yml is active on "+preflight.defaultBranch+".");
        if(!"success".equalsIgnoreCase(conclusion)){
            String failure=fetchFailureSummary(owner,repo,token,runId);
            return new BuildReceipt(branch,runId,runUrl,null,null,conclusion,failure);
        }

        String artifacts=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs/"+runId+"/artifacts?per_page=20",token,null,false,"generated APK artifacts");
        String artifactName=firstValue(artifacts,"name");
        String artifactApi=firstValue(artifacts,"archive_download_url");
        if(artifactName==null) throw new IllegalStateException("ARTIFACT ERROR: CI succeeded, but no generated APK artifact was uploaded for run "+runId+".");

        progress.onProgress("APK ready","CI succeeded and uploaded "+artifactName+".");
        return new BuildReceipt(branch,runId,runUrl,artifactName,artifactApi,"success",null);
    }

    private PreflightReceipt preflight(String owner,String repo,String token,Progress progress) throws Exception {
        progress.onProgress("GitHub preflight","Checking repository access, token permissions, branch, workflow, and Actions visibility.");

        String repoJson=request("GET","https://api.github.com/repos/"+owner+"/"+repo,token,null,false,"repository preflight");
        String defaultBranch=value(repoJson,"default_branch");
        if(defaultBranch==null||defaultBranch.trim().isEmpty()) defaultBranch="main";

        String refJson=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/git/ref/heads/"+url(defaultBranch),token,null,false,"default branch "+defaultBranch);
        String parentSha=nestedSha(refJson);
        if(parentSha==null) throw new IllegalStateException("BRANCH ERROR: Could not resolve default branch '"+defaultBranch+"'.");

        String workflowJson=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/generated-project.yml",token,null,false,"trusted generated-project workflow");
        String workflowState=value(workflowJson,"state");
        String workflowName=value(workflowJson,"name");
        if(workflowState!=null&&!"active".equalsIgnoreCase(workflowState)) throw new IllegalStateException("WORKFLOW ERROR: generated-project.yml exists but is not active (state: "+workflowState+").");

        // This GET is deliberate: private repositories require Actions: read for the
        // observation path. A 403 here becomes a permission-specific preflight error
        // before AIDao uploads any generated source.
        request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/generated-project.yml/runs?per_page=1",token,null,false,"Actions read preflight");

        progress.onProgress("GitHub preflight","Passed. Default branch: "+defaultBranch+" · trusted workflow: "+(workflowName==null?"generated-project.yml":workflowName)+" · token minimum: "+TOKEN_PERMISSION_SUMMARY+".");
        return new PreflightReceipt(defaultBranch,parentSha,workflowName);
    }

    private String fetchFailureSummary(String owner,String repo,String token,long runId){
        try{
            String jobs=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs/"+runId+"/jobs?per_page=20",token,null,false,"failed build jobs");
            long jobId=firstLong(jobs,"id");
            String jobName=firstValue(jobs,"name");
            String conclusion=firstValue(jobs,"conclusion");
            return "BUILD ERROR: CI failure in "+(jobName==null?"build":jobName)+(conclusion==null?"":" ("+conclusion+")")+". Run ID "+runId+", job ID "+jobId+".";
        }catch(Exception e){
            return "BUILD ERROR: CI failed for run "+runId+". Detailed job lookup was unavailable: "+e.getMessage();
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

    private RunMatch findRunForBranch(String json,String branch){
        Pattern p=Pattern.compile("\\{[^{}]*\\\"id\\\"\\s*:\\s*(\\d+)[^{}]*\\\"display_title\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"[^{}]*\\\"status\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"[^{}]*?(?:\\\"conclusion\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|null))?[^{}]*?\\\"html_url\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"[^{}]*\\}");
        Matcher m=p.matcher(json);
        while(m.find()){
            if(!m.group(2).contains(branch)) continue;
            RunMatch r=new RunMatch();
            r.id=Long.parseLong(m.group(1));
            r.status=m.group(3);
            r.conclusion=m.group(4);
            r.url=m.group(5);
            return r;
        }
        // GitHub may reorder object properties. Fall back to a bounded object slice
        // around the target branch and parse fields independently.
        int at=json.indexOf(branch);
        if(at<0) return null;
        int start=Math.max(0,json.lastIndexOf("{\"id\"",at));
        int end=json.indexOf("},{\"id\"",at);
        if(end<0) end=Math.min(json.length(),at+5000);
        String slice=json.substring(start,Math.min(json.length(),end));
        RunMatch r=new RunMatch();
        r.id=firstLong(slice,"id");
        r.url=value(slice,"html_url");
        r.status=value(slice,"status");
        r.conclusion=value(slice,"conclusion");
        return r.id>0?r:null;
    }

    private void requireRepo(String v){
        if(v==null||!v.trim().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("REPOSITORY ERROR: Repository must use owner/name format.");
    }

    private String request(String method,String u,String token,String body,boolean allowEmpty,String operation)throws Exception{
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
        String accepted=c.getHeaderField("X-Accepted-GitHub-Permissions");
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
            String acceptedText=(accepted==null||accepted.trim().isEmpty())?"":" GitHub accepted-permissions hint: "+accepted+".";
            if(code==401) throw new IllegalStateException("AUTH 401 during "+operation+": GitHub rejected the token. Create a current fine-grained token for this repository. Minimum: "+TOKEN_PERMISSION_SUMMARY+"."+acceptedText);
            if(code==403) throw new IllegalStateException("PERMISSION 403 during "+operation+": GitHub denied the request. Minimum fine-grained permissions for AIDao are "+TOKEN_PERMISSION_SUMMARY+"; Workflows write is not required."+acceptedText+" GitHub detail: "+detail);
            if(code==404){
                if(operation.contains("workflow")||operation.contains("Actions")) throw new IllegalStateException("WORKFLOW 404 during "+operation+": trusted .github/workflows/generated-project.yml was not visible on the repository default branch, or the token cannot access Actions. GitHub detail: "+detail);
                if(operation.contains("branch")) throw new IllegalStateException("BRANCH 404 during "+operation+": the requested repository branch/ref was not found. GitHub detail: "+detail);
                throw new IllegalStateException("REPOSITORY 404 during "+operation+": repository/resource not found or token repository access is missing. GitHub detail: "+detail);
            }
            throw new IllegalStateException("GITHUB API "+code+" during "+operation+": "+detail+acceptedText);
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
