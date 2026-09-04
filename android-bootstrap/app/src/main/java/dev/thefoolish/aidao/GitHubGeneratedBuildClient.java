package dev.thefoolish.aidao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
 * v1 transport note: generated files are uploaded with GitHub's repository
 * Contents API under a dedicated project root on the isolated branch. This
 * deliberately avoids the low-level create-blob/create-tree path that returned
 * "Resource not accessible by personal access token" for a valid fine-grained
 * token during the v0.5 on-device test.
 *
 * Fine-grained token minimum for the full on-device path:
 * - repository Contents: read/write (branch/source upload + repository_dispatch)
 * - repository Actions: read (workflow/run/job/artifact observation)
 * Metadata read is implicit. Workflows write is intentionally not required.
 */
final class GitHubGeneratedBuildClient {
    static final String TOKEN_PERMISSION_SUMMARY = "Contents: read/write + Actions: read";
    static final String GENERATED_PROJECT_ROOT = ".aidao-generated-project";

    static final class BuildReceipt {
        final String branch;
        final long runId;
        final String runUrl;
        final String artifactName;
        final String artifactUrl;
        final String conclusion;
        final String failureSummary;
        final String repoFullName;
        final String projectName;
        final String sourceSha;
        final long artifactId;

        BuildReceipt(String branch,long runId,String runUrl,String artifactName,String artifactUrl,String conclusion,String failureSummary,
                     String repoFullName,String projectName,String sourceSha,long artifactId){
            this.branch=branch;
            this.runId=runId;
            this.runUrl=runUrl;
            this.artifactName=artifactName;
            this.artifactUrl=artifactUrl;
            this.conclusion=conclusion;
            this.failureSummary=failureSummary;
            this.repoFullName=repoFullName;
            this.projectName=projectName;
            this.sourceSha=sourceSha;
            this.artifactId=artifactId;
        }

        boolean success(){
            return "success".equalsIgnoreCase(conclusion)
                && artifactName!=null
                && artifactId>0
                && sourceSha!=null
                && !sourceSha.trim().isEmpty();
        }
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

    private static final class ArtifactMatch {
        long id;
        String name;
        String archiveUrl;
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
            if(f.path.startsWith("/")||f.path.contains("../")||f.path.equals("..")) {
                throw new IllegalStateException("SOURCE ERROR: Refusing unsafe generated path: "+f.path);
            }
            sourceFiles.add(f);
        }
        if(sourceFiles.isEmpty()) throw new IllegalStateException("SOURCE ERROR: Generated project contains no source files to upload.");

        String branch="aidao-generated-"+slug(project.projectName)+"-"+(System.currentTimeMillis()/1000L);
        progress.onProgress("Creating generated branch","Creating isolated branch "+branch+" from "+preflight.defaultBranch+".");
        request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/git/refs",token,
            "{\"ref\":\"refs/heads/"+json(branch)+"\",\"sha\":\""+json(preflight.parentSha)+"\"}",false,"generated branch "+branch);

        progress.onProgress("Uploading generated source",sourceFiles.size()+" files via GitHub Contents API to "+GENERATED_PROJECT_ROOT+" on the isolated branch.");
        int uploaded=0;
        for(GeneratedProject.FileEntry f:sourceFiles){
            String destination=GENERATED_PROJECT_ROOT+"/"+f.path;
            String encoded=Base64.getEncoder().encodeToString(f.content.getBytes(StandardCharsets.UTF_8));
            String body="{\"message\":\"AIDao generated: "+json(f.path)+"\",\"content\":\""+encoded+"\",\"branch\":\""+json(branch)+"\"}";
            request("PUT","https://api.github.com/repos/"+owner+"/"+repo+"/contents/"+contentPath(destination),token,body,false,"generated file "+f.path);
            uploaded++;
            if(uploaded==sourceFiles.size()||uploaded%5==0){
                progress.onProgress("Uploading generated source",uploaded+" / "+sourceFiles.size()+" files uploaded.");
            }
        }

        String generatedRef=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/git/ref/heads/"+url(branch),token,null,false,"generated source revision");
        String sourceSha=nestedSha(generatedRef);
        if(sourceSha==null||sourceSha.trim().isEmpty()) {
            throw new IllegalStateException("SOURCE IDENTITY ERROR: Generated branch source SHA is unavailable; build handoff was not started.");
        }
        String exactRepo=owner+"/"+repo;
        progress.onProgress("Source identity","Pinned generated source "+shortSha(sourceSha)+" on "+branch+".");

        progress.onProgress("Starting Android CI","Generated source uploaded. Triggering trusted workflow from "+preflight.defaultBranch+".");
        String dispatch="{\"event_type\":\"aidao-generated-build\",\"client_payload\":{"
            +"\"target_branch\":\""+json(branch)+"\","
            +"\"project_name\":\""+json(project.projectName)+"\","
            +"\"project_root\":\""+GENERATED_PROJECT_ROOT+"\","
            +"\"source_sha\":\""+json(sourceSha)+"\","
            +"\"repository\":\""+json(exactRepo)+"\"}}";
        request("POST","https://api.github.com/repos/"+owner+"/"+repo+"/dispatches",token,dispatch,true,"repository_dispatch");

        long runId=0;
        String runUrl=null,conclusion=null;
        long deadline=System.currentTimeMillis()+12*60*1000L;
        while(System.currentTimeMillis()<deadline){
            Thread.sleep(6000);
            String runs=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/workflows/generated-project.yml/runs?event=repository_dispatch&per_page=20",token,null,false,"generated workflow runs");
            RunMatch match=findRunForBranch(runs,branch,sourceSha);
            if(match==null) continue;
            runId=match.id;
            runUrl=match.url;
            conclusion=match.conclusion;
            progress.onProgress("Android CI",match.status==null?"Workflow discovered":"Workflow "+match.status+(conclusion==null?"":" · "+conclusion));
            if(runId>0&&"completed".equalsIgnoreCase(match.status)) break;
        }

        if(runId==0) throw new IllegalStateException("WORKFLOW ERROR: repository_dispatch was accepted, but no run matching both branch "+branch+" and source "+shortSha(sourceSha)+" appeared within 12 minutes. Confirm Actions are enabled and generated-project.yml is active on "+preflight.defaultBranch+".");
        if(!"success".equalsIgnoreCase(conclusion)){
            String failure=fetchFailureSummary(owner,repo,token,runId);
            return new BuildReceipt(branch,runId,runUrl,null,null,conclusion,failure,exactRepo,project.projectName,sourceSha,0L);
        }

        String artifacts=request("GET","https://api.github.com/repos/"+owner+"/"+repo+"/actions/runs/"+runId+"/artifacts?per_page=20",token,null,false,"generated APK artifacts");
        String expectedArtifact="aidao-generated-apk-"+runId;
        ArtifactMatch artifact=findArtifact(artifacts,expectedArtifact);
        if(artifact==null) throw new IllegalStateException("ARTIFACT ERROR: CI succeeded, but exact artifact "+expectedArtifact+" was not uploaded for run "+runId+".");

        progress.onProgress("APK ready","CI built source "+shortSha(sourceSha)+" and uploaded exact artifact "+artifact.name+" (#"+artifact.id+").");
        return new BuildReceipt(branch,runId,runUrl,artifact.name,artifact.archiveUrl,"success",null,exactRepo,project.projectName,sourceSha,artifact.id);
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

    private RunMatch findRunForBranch(String json,String branch,String sourceSha){
        if(json==null||branch==null||sourceSha==null) return null;
        int searchFrom=0;
        while(searchFrom<json.length()){
            int at=json.indexOf(branch,searchFrom);
            if(at<0) return null;
            int start=json.lastIndexOf("{\"id\"",at);
            if(start<0) start=Math.max(0,at-4000);
            int end=json.indexOf("},{\"id\"",at);
            if(end<0) end=Math.min(json.length(),at+7000);
            String slice=json.substring(start,Math.min(json.length(),end));
            if(slice.contains(sourceSha)){
                RunMatch r=new RunMatch();
                r.id=firstLong(slice,"id");
                r.url=value(slice,"html_url");
                r.status=value(slice,"status");
                r.conclusion=value(slice,"conclusion");
                if(r.id>0) return r;
            }
            searchFrom=at+branch.length();
        }
        return null;
    }

    private ArtifactMatch findArtifact(String json,String expectedName){
        if(json==null||expectedName==null) return null;
        int at=json.indexOf("\"name\":\""+expectedName+"\"");
        if(at<0) return null;
        int start=json.lastIndexOf('{',at);
        if(start<0) return null;
        int end=json.indexOf('}',at);
        if(end<0) end=Math.min(json.length(),at+3000);
        String slice=json.substring(start,Math.min(json.length(),end+1));
        if(slice.contains("\"expired\":true")) return null;
        ArtifactMatch a=new ArtifactMatch();
        a.id=firstLong(slice,"id");
        a.name=value(slice,"name");
        a.archiveUrl=value(slice,"archive_download_url");
        if(a.id<=0||!expectedName.equals(a.name)||a.archiveUrl==null||a.archiveUrl.trim().isEmpty()) return null;
        return a;
    }

    private void requireRepo(String v){
        if(v==null||!v.trim().matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("REPOSITORY ERROR: Repository must use owner/name format.");
    }

    private String request(String method,String u,String token,String body,boolean allowEmpty,String operation)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(12000);
        c.setReadTimeout(30000);
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
            if(code==409) throw new IllegalStateException("GITHUB CONFLICT during "+operation+": repository state changed while AIDao was uploading. Regenerate/retry to create a fresh isolated branch. GitHub detail: "+detail);
            if(code==422) throw new IllegalStateException("GITHUB VALIDATION during "+operation+": GitHub rejected the generated branch or file payload. Retry with a fresh generated branch. GitHub detail: "+detail);
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
    private String contentPath(String path)throws Exception{String[] parts=path.split("/");StringBuilder b=new StringBuilder();for(String p:parts){if(p.isEmpty())continue;if(b.length()>0)b.append('/');b.append(URLEncoder.encode(p,"UTF-8").replace("+","%20"));}return b.toString();}
    private String trim(String s,int n){return s==null?"":s.substring(0,Math.min(s.length(),n));}
    private String shortSha(String sha){return sha==null?"unknown":sha.substring(0,Math.min(12,sha.length()));}
}