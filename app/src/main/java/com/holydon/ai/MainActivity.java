package com.holydon.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final String PREFS="holydon_ai";
    private static final String HISTORY="chat_history";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LinearLayout root,body,messages,composerBar;
    private ScrollView chatScroll;
    private EditText composer;
    private TextView connectionLabel;
    private String page="CHAT";
    private Uri selectedFile;
    private boolean busy=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8,12,22));
        getWindow().setNavigationBarColor(Color.rgb(8,12,22));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        renderApp();
    }
    private int color(String s){return Color.parseColor(s);}
    private GradientDrawable shape(String hex,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color(hex));d.setCornerRadius(dp(radius));return d;
    }
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private LinearLayout horizontal(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String s,int size,String hex){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color(hex));
        t.setPadding(dp(4),dp(5),dp(4),dp(5));return t;
    }
    private TextView heading(String s){
        TextView t=text(s,25,"#F5F7FF");t.setTypeface(null,Typeface.BOLD);
        t.setPadding(dp(2),dp(12),dp(2),dp(5));return t;
    }
    private TextView muted(String s){return text(s,13,"#9AA8C0");}
    private Button button(String s){
        Button b=new Button(this);b.setText(s);b.setTextColor(color("#07111F"));
        b.setAllCaps(false);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);
        b.setBackground(shape("#83BFFF",16));return b;
    }
    private EditText edit(String hint,int lines){
        EditText e=new EditText(this);e.setHint(hint);e.setTextColor(Color.WHITE);
        e.setHintTextColor(color("#77869F"));e.setTextSize(15);e.setPadding(dp(15),dp(12),dp(15),dp(12));
        e.setBackground(shape("#151F31",18));e.setMinLines(lines);return e;
    }
    private void renderApp(){
        root=vertical();root.setBackgroundColor(color("#080C16"));
        LinearLayout header=horizontal();header.setPadding(dp(14),dp(8),dp(14),dp(8));
        LinearLayout brand=vertical();
        TextView logo=text("HØLYDØN",19,"#F4F7FF");logo.setTypeface(null,Typeface.BOLD);
        TextView sub=text("ARTIFICIAL INTELLIGENCE",9,"#7F91AD");
        brand.addView(logo);brand.addView(sub);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView newChat=text("＋",27,"#8CC6FF");newChat.setGravity(Gravity.CENTER);
        newChat.setBackground(shape("#17263A",30));header.addView(newChat,new LinearLayout.LayoutParams(dp(46),dp(46)));
        newChat.setOnClickListener(v->newChat());
        root.addView(header);
        View line=new View(this);line.setBackgroundColor(color("#1C293B"));root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        FrameLayout frame=new FrameLayout(this);
        body=vertical();frame.addView(body,new FrameLayout.LayoutParams(-1,-1));
        root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=horizontal();nav.setPadding(dp(5),dp(5),dp(5),dp(10));nav.setMinimumHeight(dp(76));nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackgroundColor(color("#101827"));
        String[] tabs={"CHAT","TOOLS","HISTORY","SETTINGS"};
        String[] icons={"◉","✧","◷","⚙"};
        for(int i=0;i<tabs.length;i++){
            final String tab=tabs[i];LinearLayout item=vertical();item.setGravity(Gravity.CENTER);
            TextView ic=text(icons[i],18,page.equals(tab)?"#8CC6FF":"#8291A8");ic.setGravity(Gravity.CENTER);
            TextView tx=text(tabs[i],10,page.equals(tab)?"#8CC6FF":"#8291A8");tx.setGravity(Gravity.CENTER);tx.setIncludeFontPadding(false);
            item.addView(ic);item.addView(tx);
            item.setPadding(0,dp(3),0,dp(3));nav.addView(item,new LinearLayout.LayoutParams(0,dp(64),1));
            item.setOnClickListener(v->{page=tab;renderPage();});
        }
        root.addView(nav);setContentView(root);renderPage();
    }
    private void renderPage(){
        body.removeAllViews();
        if(page.equals("CHAT"))showChat();
        else if(page.equals("TOOLS"))showTools();
        else if(page.equals("HISTORY"))showHistory();
        else showSettings();
    }
    private ScrollView scroll(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);
        s.setClipToPadding(false);return s;
    }
    private LinearLayout pageContent(ScrollView s){
        LinearLayout l=vertical();l.setPadding(dp(16),dp(10),dp(16),dp(18));s.addView(l);return l;
    }
    private void showChat(){
        chatScroll=scroll();messages=vertical();messages.setPadding(dp(12),dp(12),dp(12),dp(12));
        chatScroll.addView(messages);body.addView(chatScroll,new LinearLayout.LayoutParams(-1,0,1));
        JSONArray history=loadHistory();
        if(history.length()==0){
            welcome();
        }else{
            for(int i=0;i<history.length();i++){
                JSONObject m=history.optJSONObject(i);if(m!=null)addBubble(m.optString("role"),m.optString("text"),false);
            }
        }
        composerBar=horizontal();composerBar.setPadding(dp(12),dp(8),dp(12),dp(8));
        composerBar.setBackgroundColor(color("#0D1421"));
        composer=edit("Message HOLYDON AI…",1);composer.setMaxLines(5);
        composerBar.addView(composer,new LinearLayout.LayoutParams(0,-2,1));
        TextView send=text("➤",20,"#07111F");send.setGravity(Gravity.CENTER);send.setBackground(shape("#83BFFF",30));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(46),dp(46));sp.setMargins(dp(8),0,0,0);
        composerBar.addView(send,sp);send.setOnClickListener(v->sendChat("chat"));
        body.addView(composerBar);
        connectionLabel=text(isConfigured()?"● BACKEND URL SAVED":"● AI CONNECTION NOT SET",10,isConfigured()?"#62DDB8":"#F0C477");
        connectionLabel.setPadding(dp(16),dp(3),dp(16),dp(5));body.addView(connectionLabel);
        if(!isConfigured()){
            TextView hint=text("Set your private backend URL in Settings to enable live AI replies.",11,"#8494AD");
            hint.setPadding(dp(16),0,dp(16),dp(7));body.addView(hint);
        }
    }
    private void welcome(){
        LinearLayout panel=vertical();panel.setPadding(dp(18),dp(20),dp(18),dp(20));
        panel.setBackground(shape("#111C2D",22));
        TextView mark=text("✦",30,"#8CC6FF");mark.setGravity(Gravity.CENTER);
        panel.addView(mark);
        TextView title=heading("What can I help with?");
        title.setGravity(Gravity.CENTER);panel.addView(title);
        TextView desc=text("Ask questions, learn something new, write code, or work with your files.",14,"#AAB8CD");
        desc.setGravity(Gravity.CENTER);panel.addView(desc);
        messages.addView(panel);
        messages.addView(text("QUICK START",10,"#8092AC"));
        quickPrompt("Explain something to me","Explain a difficult topic in simple steps.");
        quickPrompt("Help me write","Help me write a clear and professional message.");
        quickPrompt("Build or debug code","Help me build an Android app in Java.");
    }
    private void quickPrompt(String title,String prompt){
        TextView t=text("✧  "+title+"     ›",14,"#DDE8F8");
        t.setPadding(dp(14),dp(13),dp(14),dp(13));t.setBackground(shape("#121D2D",15));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));messages.addView(t,p);
        t.setOnClickListener(v->{if(composer!=null){composer.setText(prompt);composer.setSelection(prompt.length());}});
    }
    private void addBubble(String role,String value,boolean persist){
        boolean user=role.equalsIgnoreCase("user")||role.equalsIgnoreCase("YOU");
        LinearLayout card=vertical();card.setPadding(dp(13),dp(9),dp(13),dp(10));
        card.setBackground(shape(user?"#19304A":"#121B2A",18));
        TextView who=text(user?"YOU":"HOLYDON AI",10,user?"#8CC6FF":"#65DDB9");
        who.setTypeface(null,Typeface.BOLD);card.addView(who);
        TextView msg=text(value,15,"#F1F5FC");msg.setPadding(dp(3),dp(5),dp(3),dp(3));card.addView(msg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));messages.addView(card,p);
        if(persist)saveMessage(user?"user":"assistant",value);
        if(chatScroll!=null)chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
    }
    private JSONArray loadHistory(){
        try{return new JSONArray(getSharedPreferences(PREFS,0).getString(HISTORY,"[]"));}
        catch(Exception e){return new JSONArray();}
    }
    private void saveMessage(String role,String value){
        JSONArray a=loadHistory();JSONObject o=new JSONObject();
        try{o.put("role",role);o.put("text",value);a.put(o);}catch(Exception ignored){}
        getSharedPreferences(PREFS,0).edit().putString(HISTORY,a.toString()).apply();
    }
    private void newChat(){
        getSharedPreferences(PREFS,0).edit().remove(HISTORY).apply();
        page="CHAT";renderPage();
    }
    private void sendChat(String mode){
        if(busy)return;
        String prompt=composer==null?"":composer.getText().toString().trim();
        if(prompt.isEmpty())return;
        if(!isConfigured()){
            Toast.makeText(this,"Add your backend URL in Settings first.",Toast.LENGTH_LONG).show();return;
        }
        addBubble("user",prompt,true);composer.setText("");
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(),0);
        busy=true;connectionLabel.setText("● HOLYDON AI IS THINKING…");
        JSONObject data=new JSONObject();
        try{
            data.put("message",prompt);data.put("mode",mode);
            JSONArray saved=loadHistory();JSONArray context=new JSONArray();
            int first=Math.max(0,saved.length()-20);
            for(int i=first;i<saved.length();i++){JSONObject item=saved.optJSONObject(i);if(item==null)continue;JSONObject turn=new JSONObject();turn.put("role",item.optString("role","user"));turn.put("content",item.optString("text",""));context.put(turn);}
            data.put("messages",context);
        }catch(Exception ignored){}
        request("/api/chat",data,(result,error)->{
            busy=false;
            if(error==null){addBubble("assistant",result,true);connectionLabel.setText("● RESPONSE RECEIVED");}
            else{addBubble("assistant","I couldn't reach the AI service. "+error+"\n\nYour message is saved. Check the backend URL and service status in Settings.",true);connectionLabel.setText("● CONNECTION ISSUE");}
        });
    }
    private void showTools(){
        ScrollView s=scroll();LinearLayout l=pageContent(s);body.addView(s);
        l.addView(heading("AI tools"));l.addView(muted("A workspace for more than just chatting."));
        toolCard(l,"✧","IMAGE STUDIO","Generate images and edit pictures with instructions.","IMAGE");
        toolCard(l,"▤","PDF INTELLIGENCE","Upload a document and ask questions about its contents.","PDF");
        toolCard(l,"⌘","CODE ASSISTANT","Write, explain, review and debug code.","CODE");
    }
    private void toolCard(LinearLayout parent,String icon,String title,String desc,String target){
        LinearLayout card=vertical();card.setPadding(dp(16),dp(13),dp(16),dp(13));card.setBackground(shape("#121D2D",18));
        TextView a=text(icon+"   "+title,15,"#EAF2FF");a.setTypeface(null,Typeface.BOLD);card.addView(a);
        card.addView(muted(desc));TextView go=text("OPEN TOOL  →",10,"#8CC6FF");card.addView(go);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(9),0,dp(4));parent.addView(card,p);
        card.setOnClickListener(v->openTool(target));
    }
    private void openTool(String target){
        page=target;renderToolPage();
    }
    private void renderToolPage(){
        body.removeAllViews();ScrollView s=scroll();LinearLayout l=pageContent(s);body.addView(s);
        if(page.equals("IMAGE"))showImage(l);
        else if(page.equals("PDF"))showPdf(l);
        else showCode(l);
    }
    private void showImage(LinearLayout l){
        l.addView(heading("Image studio"));l.addView(muted("Create an image from a text description. An image provider must be configured on your backend."));
        EditText prompt=edit("Describe the image you want to create…",4);l.addView(prompt);
        Button run=button("GENERATE IMAGE");l.addView(run);
        ImageView preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setScaleType(ImageView.ScaleType.FIT_CENTER);preview.setBackground(shape("#111C2D",18));
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,dp(300));imageParams.setMargins(0,dp(12),0,dp(8));l.addView(preview,imageParams);
        TextView status=muted("Your generated image will appear here.");l.addView(status);
        run.setOnClickListener(v->{String description=prompt.getText().toString().trim();if(description.isEmpty()){Toast.makeText(this,"Describe the image first.",0).show();return;}status.setText("Generating image…");run.setEnabled(false);generateImage(description,preview,status,run);});
        l.addView(muted("Image editing from an uploaded photo is not enabled yet. This tool generates new images only."));
    }
    private void showPdf(LinearLayout l){
        l.addView(heading("PDF intelligence"));l.addView(muted("Upload a PDF and ask questions about its actual text. Maximum 12 MB."));
        Button pick=button("SELECT PDF DOCUMENT");l.addView(pick);
        TextView file=text(selectedFile==null?"No document selected":selectedFile.getLastPathSegment(),12,"#AAB8CD");l.addView(file);
        EditText q=edit("Ask a question about this PDF…",3);l.addView(q);
        Button analyse=button("ANALYSE DOCUMENT");l.addView(analyse);
        TextView result=muted("Your document answer will appear here.");l.addView(result);
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("application/pdf");startActivityForResult(i,43);});
        analyse.setOnClickListener(v->{if(selectedFile==null){Toast.makeText(this,"Select a PDF first.",0).show();return;}String question=q.getText().toString().trim();if(question.isEmpty())question="Summarise this document and identify its key points.";result.setText("Uploading and reading PDF…");uploadPdf(selectedFile,question,(answer,error)->result.setText(error==null?answer:"PDF analysis failed: "+error));});
    }
    private void showCode(LinearLayout l){
        l.addView(heading("Code assistant"));l.addView(muted("Plan, write, explain and debug code with HOLYDON AI."));
        EditText task=edit("Describe the app, feature or bug…",5);l.addView(task);
        Button ask=button("ASK CODE ASSISTANT");l.addView(ask);
        ask.setOnClickListener(v->{String q=task.getText().toString().trim();if(q.isEmpty())return;page="CHAT";renderPage();composer.setText(q);sendChat("code");});
    }
    private void showHistory(){
        ScrollView s=scroll();LinearLayout l=pageContent(s);body.addView(s);
        l.addView(heading("Your conversations"));l.addView(muted("Your recent messages are stored on this device."));
        JSONArray a=loadHistory();
        if(a.length()==0)l.addView(muted("No conversations yet. Start a new chat to begin."));
        else{
            for(int i=a.length()-1;i>=0;i--){
                JSONObject o=a.optJSONObject(i);if(o==null)continue;
                String role=o.optString("role");String val=o.optString("text");
                TextView row=text((role.equals("user")?"YOU  ·  ":"HOLYDON AI  ·  ")+val,13,"#DCE7F7");
                row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setBackground(shape("#121D2D",14));
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));l.addView(row,p);
            }
        }
        Button clear=button("CLEAR CHAT HISTORY");l.addView(clear);
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Clear history?").setMessage("This removes saved conversations from this device.").setNegativeButton("CANCEL",null).setPositiveButton("CLEAR",(d,w)->{getSharedPreferences(PREFS,0).edit().remove(HISTORY).apply();renderPage();}).show());
    }
    private boolean isConfigured(){return !getSharedPreferences(PREFS,0).getString("api_base","").trim().isEmpty();}
    private String baseUrl(){return getSharedPreferences(PREFS,0).getString("api_base","").trim().replaceAll("/+$","");}
    private void showSettings(){
        ScrollView s=scroll();LinearLayout l=pageContent(s);body.addView(s);
        l.addView(heading("Settings"));l.addView(muted("Configure how HOLYDON AI connects and behaves."));
        l.addView(text("BACKEND URL",11,"#8CC6FF"));
        EditText url=edit("https://your-service.onrender.com",1);url.setText(baseUrl());l.addView(url);
        Button save=button("SAVE CONNECTION");l.addView(save);
        save.setOnClickListener(v->{String value=url.getText().toString().trim();if(!value.isEmpty()&&!value.startsWith("https://")){url.setError("Use an HTTPS address");return;}getSharedPreferences(PREFS,0).edit().putString("api_base",value).apply();Toast.makeText(this,"Connection settings saved.",0).show();});
        Button test=button("TEST BACKEND CONNECTION");l.addView(test);
        TextView result=muted("Connection has not been tested.");l.addView(result);
        test.setOnClickListener(v->{String value=url.getText().toString().trim().replaceAll("/+$","");if(value.isEmpty()){result.setText("Enter the backend URL first.");return;}result.setText("Checking service…");worker.execute(()->{String msg;HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(value+"/health").openConnection();c.setConnectTimeout(12000);c.setReadTimeout(12000);int code=c.getResponseCode();msg=code>=200&&code<300?"Backend is online (HTTP "+code+").":"Backend returned HTTP "+code+".";}catch(Exception e){msg="Could not connect: "+e.getMessage();}finally{if(c!=null)c.disconnect();}String finalMsg=msg;runOnUiThread(()->result.setText(finalMsg));});});
        l.addView(text("ABOUT HOLYDON AI",11,"#8CC6FF"));
        l.addView(muted("HOLYDON AI is your personal AI workspace. Chat history is stored locally on this device. Your backend URL is a connection address, not an API key."));
        l.addView(muted("Live chat requires a deployed backend. Image generation requires an image provider. PDF analysis requires the deployed backend and its PDF extraction package."));
    }

    private void generateImage(String prompt,ImageView preview,TextView status,Button run){
        worker.execute(()->{
            HttpURLConnection conn=null;android.graphics.Bitmap bitmap=null;String error=null;
            try{
                JSONObject data=new JSONObject();data.put("prompt",prompt);
                conn=(HttpURLConnection)new URL(baseUrl()+"/api/image").openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(120000);conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                try(OutputStream out=conn.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}
                int code=conn.getResponseCode();String type=conn.getContentType()==null?"":conn.getContentType().toLowerCase();
                if(code<200||code>=300){InputStream in=conn.getErrorStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(in!=null){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);}try{error=new JSONObject(bytes.toString("UTF-8")).optString("error","Image provider returned HTTP "+code);}catch(Exception ignored){error="Image provider returned HTTP "+code;}}
                else if(type.startsWith("image/")){try(InputStream in=conn.getInputStream()){bitmap=BitmapFactory.decodeStream(in);}if(bitmap==null)error="The image provider returned an unreadable image.";}
                else{InputStream in=conn.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);try{error=new JSONObject(bytes.toString("UTF-8")).optString("error","Provider did not return an image.");}catch(Exception ignored){error="Provider did not return an image file.";}}
            }catch(Exception e){error=e.getMessage()==null?"Image generation failed":e.getMessage();}finally{if(conn!=null)conn.disconnect();}
            final android.graphics.Bitmap image=bitmap;final String problem=error;runOnUiThread(()->{run.setEnabled(true);if(image!=null){preview.setImageBitmap(image);status.setText("Image generated successfully.");}else status.setText("Could not generate image: "+problem);});
        });
    }
    private void uploadPdf(Uri uri,String question,Callback cb){
        worker.execute(()->{
            HttpURLConnection conn=null;String answer=null,error=null;
            String boundary="----HOLYDON"+System.currentTimeMillis();
            try{
                byte[] pdf;
                try(InputStream input=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    if(input==null)throw new Exception("Cannot open the selected document.");
                    byte[] buf=new byte[8192];int n;while((n=input.read(buf))!=-1){out.write(buf,0,n);if(out.size()>12*1024*1024)throw new Exception("PDF is larger than 12 MB.");}pdf=out.toByteArray();
                }
                conn=(HttpURLConnection)new URL(baseUrl()+"/api/pdf").openConnection();conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(90000);conn.setDoOutput(true);conn.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
                try(OutputStream out=conn.getOutputStream()){
                    writePart(out,boundary,"question",null,question.getBytes(StandardCharsets.UTF_8));
                    writePart(out,boundary,"file","document.pdf",pdf);
                    out.write(("--"+boundary+"--\\r\\n").getBytes(StandardCharsets.UTF_8));
                }
                int code=conn.getResponseCode();InputStream in=code>=400?conn.getErrorStream():conn.getInputStream();ByteArrayOutputStream bytes=new ByteArrayOutputStream();if(in!=null){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);}JSONObject json=new JSONObject(bytes.toString("UTF-8"));if(code<200||code>=300)throw new Exception(json.optString("error","Server error HTTP "+code));answer=json.optString("reply","No answer returned.");
            }catch(Exception e){error=e.getMessage()==null?"Unknown upload error":e.getMessage();}finally{if(conn!=null)conn.disconnect();}
            final String a=answer,er=error;runOnUiThread(()->cb.done(a,er));
        });
    }
    private void writePart(OutputStream out,String boundary,String name,String filename,byte[] data)throws IOException{
        String head="--"+boundary+"\r\nContent-Disposition: form-data; name="+name+(filename==null?"":"; filename="+filename)+"\r\n"+(filename==null?"Content-Type: text/plain; charset=utf-8\r\n":"Content-Type: application/pdf\r\n")+"\r\n";
        out.write(head.getBytes(StandardCharsets.UTF_8));out.write(data);out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
    interface Callback{void done(String result,String error);}
    private void request(String path,JSONObject data,Callback cb){
        worker.execute(()->{
            HttpURLConnection conn=null;String answer=null,error=null;
            try{
                URL url=new URL(baseUrl()+path);conn=(HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");conn.setConnectTimeout(20000);conn.setReadTimeout(60000);
                conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
                try(OutputStream out=conn.getOutputStream()){out.write(data.toString().getBytes(StandardCharsets.UTF_8));}
                int code=conn.getResponseCode();InputStream in=code>=400?conn.getErrorStream():conn.getInputStream();
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();
                if(in!=null){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)bytes.write(buf,0,n);}
                String raw=bytes.toString("UTF-8");
                if(raw.trim().isEmpty())throw new Exception("The server returned an empty response (HTTP "+code+").");
                JSONObject json;
                try{json=new JSONObject(raw);}catch(Exception parse){throw new Exception("The server returned an unreadable response (HTTP "+code+"). Check that the backend URL points to the HOLYDON AI API.");}
                if(code<200||code>=300)throw new Exception(json.optString("error","Server error HTTP "+code));
                answer=json.optString("reply",json.optString("message","No reply was returned by the server."));
            }catch(Exception e){error=e.getMessage()==null?"Unknown connection error":e.getMessage();}
            finally{if(conn!=null)conn.disconnect();}
            final String a=answer,er=error;runOnUiThread(()->cb.done(a,er));
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result==RESULT_OK&&data!=null){selectedFile=data.getData();Toast.makeText(this,"File selected: "+selectedFile.getLastPathSegment(),Toast.LENGTH_SHORT).show();if(page.equals("PDF"))renderToolPage();}
    }
    @Override public void onBackPressed(){
        if(page.equals("IMAGE")||page.equals("PDF")||page.equals("CODE")){page="TOOLS";renderPage();}
        else if(!page.equals("CHAT")){page="CHAT";renderPage();}
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
