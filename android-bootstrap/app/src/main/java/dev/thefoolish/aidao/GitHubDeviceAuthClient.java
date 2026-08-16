package dev.thefoolish.aidao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub App device-flow authorization for AIDao's Android client.
 *
 * This flow intentionally requires only the public GitHub App client ID; no
 * client secret is embedded in the APK. The returned GitHub App user access
 * token is intended to remain session-only in AIDao and is bounded by both the
 * user's permissions and the repositories selected for the GitHub App
 * installation.
 */
final class GitHubDeviceAuthClient {
    static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    static final String DEFAULT_VERIFICATION_URL = "https://github.com/login/device";
    static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    static final class DeviceCode {
        final String deviceCode;
        final String userCode;
        final String verificationUri;
        final long expiresAtMillis;
        final int intervalSeconds;

        DeviceCode(String deviceCode,String userCode,String verificationUri,long expiresAtMillis,int intervalSeconds){
            this.deviceCode=deviceCode;
            this.userCode=userCode;
            this.verificationUri=verificationUri;
            this.expiresAtMillis=expiresAtMillis;
            this.intervalSeconds=Math.max(5,intervalSeconds);
        }

        boolean expired(){ return System.currentTimeMillis()>=expiresAtMillis; }
    }

    static final class TokenResult {
        enum State { AUTHORIZED, PENDING, SLOW_DOWN, DENIED, EXPIRED }
        final State state;
        final String accessToken;
        final String tokenType;
        final String errorDescription;
        final int nextIntervalSeconds;

        TokenResult(State state,String accessToken,String tokenType,String errorDescription,int nextIntervalSeconds){
            this.state=state;
            this.accessToken=accessToken;
            this.tokenType=tokenType;
            this.errorDescription=errorDescription;
            this.nextIntervalSeconds=nextIntervalSeconds;
        }

        boolean authorized(){ return state==State.AUTHORIZED && accessToken!=null && !accessToken.isEmpty(); }
    }

    DeviceCode begin(String clientId) throws Exception {
        requireClientId(clientId);
        String body="client_id="+form(clientId.trim());
        String json=postForm(DEVICE_CODE_URL,body);
        String device=value(json,"device_code");
        String user=value(json,"user_code");
        String uri=value(json,"verification_uri");
        int expires=number(json,"expires_in",900);
        int interval=number(json,"interval",5);
        if(device==null||user==null) throw new IllegalStateException("GitHub device authorization did not return a usable device code.");
        if(uri==null||uri.trim().isEmpty()) uri=DEFAULT_VERIFICATION_URL;
        return new DeviceCode(device,user,uri,System.currentTimeMillis()+expires*1000L,interval);
    }

    TokenResult pollOnce(String clientId,DeviceCode code,int currentIntervalSeconds) throws Exception {
        requireClientId(clientId);
        if(code==null) throw new IllegalArgumentException("Device authorization session is required.");
        if(code.expired()) return new TokenResult(TokenResult.State.EXPIRED,null,null,"The GitHub authorization code expired.",Math.max(5,currentIntervalSeconds));
        String body="client_id="+form(clientId.trim())+
            "&device_code="+form(code.deviceCode)+
            "&grant_type="+form(GRANT_TYPE);
        String json=postForm(ACCESS_TOKEN_URL,body);
        String token=value(json,"access_token");
        if(token!=null&&!token.isEmpty()) return new TokenResult(TokenResult.State.AUTHORIZED,token,value(json,"token_type"),null,Math.max(code.intervalSeconds,currentIntervalSeconds));

        String error=value(json,"error");
        String description=value(json,"error_description");
        int next=Math.max(code.intervalSeconds,currentIntervalSeconds);
        if("authorization_pending".equals(error)) return new TokenResult(TokenResult.State.PENDING,null,null,description,next);
        if("slow_down".equals(error)) return new TokenResult(TokenResult.State.SLOW_DOWN,null,null,description,next+5);
        if("access_denied".equals(error)) return new TokenResult(TokenResult.State.DENIED,null,null,description,next);
        if("expired_token".equals(error)) return new TokenResult(TokenResult.State.EXPIRED,null,null,description,next);
        throw new IllegalStateException("GitHub authorization failed"+(description==null?"":": "+description)+(error==null?"":" ["+error+"]"));
    }

    private String postForm(String endpoint,String body) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(12000);
        c.setReadTimeout(25000);
        c.setDoOutput(true);
        c.setRequestProperty("Accept","application/json");
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=utf-8");
        c.setRequestProperty("User-Agent","AIDao-Android");
        try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}
        int status=c.getResponseCode();
        java.io.InputStream stream=(status>=200&&status<300)?c.getInputStream():c.getErrorStream();
        StringBuilder b=new StringBuilder();
        if(stream!=null){
            BufferedReader r=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8));
            String line; while((line=r.readLine())!=null)b.append(line); r.close();
        }
        c.disconnect();
        if(status<200||status>=300) throw new IllegalStateException("GitHub authorization HTTP "+status+": "+trim(b.toString(),500));
        return b.toString();
    }

    private void requireClientId(String clientId){
        if(clientId==null||clientId.trim().isEmpty()) throw new IllegalArgumentException("AIDao GitHub App client ID is not configured.");
    }
    private String form(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
    private String value(String json,String key){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);return m.find()?m.group(1):null;}
    private int number(String json,String key,int fallback){Matcher m=Pattern.compile("\\\""+Pattern.quote(key)+"\\\"\\s*:\\s*(\\d+)").matcher(json);return m.find()?Integer.parseInt(m.group(1)):fallback;}
    private String trim(String s,int n){return s==null?"":s.substring(0,Math.min(s.length(),n));}
}
