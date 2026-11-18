package com.holydon.ai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
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
    private static final String API_BASE = "https://YOUR-HOLYDON-BACKEND.onrender.com";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout root, content, messages;
    private EditText composer;
    private TextView status;
    private String page = "CHAT";
    private Uri selectedFile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8,10,18));
        getWindow().setNavigationBarColor(Color.rgb(8,10,18));
        showShell();
        showChat();
    }
    private int c(String hex) { return Color.parseColor(hex); }
    private TextView label(String s,int size,int color) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(16,12,16,12); return t;
    }
    private GradientDrawable bg(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(1); return l; }
    private void showShell() {
        root=column(); root.setBackgroundColor(c("#080A12"));
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("HØLYDØN  /  AI",21,c("#F4F7FF")); brand.setTypeface(null,Typeface.BOLD);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=label("● READY",11,c("#65E6C2")); header.addView(live);
        root.addView(header);
        ScrollView sc=new ScrollView(this); content=column(); sc.addView(content);
        root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this); nav.setBackgroundColor(c("#111625"));
        String[] tabs={"CHAT","IMAGE","PDF","CODE"};
        for(String tab:tabs){ TextView b=label(tab,11,page.equals(tab)?c("#72B8FF"):c("#A5B0C8")); b.setGravity(Gravity.CENTER);
            nav.addView(b,new LinearLayout.LayoutParams(0,58,1)); b.setOnClickListener(v->{page=tab;render();});}
        root.addView(nav); setContentView(root);
    }
    private void render(){content.removeAllViews(); if(page.equals("CHAT"))showChat(); else if(page.equals("IMAGE"))showImage(); else if(page.equals("PDF"))showPdf(); else showCode();}
    private void intro(String title,String sub){
        TextView h=label(title,25,c("#F5F7FF")); h.setTypeface(null,Typeface.BOLD); content.addView(h);
        content.addView(label(sub,13,c("#9BA8C2")));
    }
    private void showChat(){
        intro("Your AI. Your rules.","Created by HOLYDON • Ask, learn, build and explore.");
        messages=column(); messages.setPadding(8,8,8,8); content.addView(messages);
        addBubble("HOLYDON AI","I'm HOLYDON AI, created by HOLYDON. What are we working on today?",false);
        composer=new EditText(this); composer.setHint("Message HOLYDON AI…"); composer.setTextColor(c("#FFFFFF")); composer.setHintTextColor(c("#77839B"));
        composer.setBackground(bg(c("#151D2D"),22)); composer.setPadding(18,12,18,12);
        content.addView(composer,new LinearLayout.LayoutParams(-1,-2));
        Button send=button("SEND MESSAGE"); content.addView(send); send.setOnClickListener(v->sendChat(false));
        status=label("Connect your private backend to start.",11,c("#78859E")); content.addView(status);
    }
    private Button button(String text){
        Button b=new Button(this); b.setText(text); b.setTextColor(c("#07101D")); b.setAllCaps(false);
        b.setBackground(bg(c("#70B8FF"),18)); return b;
    }
    private void addBubble(String who,String text,boolean user){
        LinearLayout card=column(); card.setPadding(12,8,12,8); card.setBackground(bg(user?c("#172A42"):c("#111827"),18));
        TextView name=label(who,11,user?c("#79C0FF"):c("#76E3C2")); name.setTypeface(null,Typeface.BOLD);
        card.addView(name); TextView body=label(text,15,c("#F1F5FC")); card.addView(body);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(4,5,4,5); messages.addView(card,p);
    }
    private void sendChat(boolean code){
        String prompt=composer.getText().toString().trim(); if(prompt.isEmpty())return;
        addBubble("YOU",prompt,true); composer.setText(""); status.setText("HOLYDON AI is thinking…");
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(composer.getWindowToken(),0);
        JSONObject body=new JSONObject(); try{body.put("message",prompt);body.put("mode",code?"code":"chat");}catch(Exception ignored){}
        request("/api/chat",body,(result,error)->{
            status.setText(error==null?"RESPONSE READY":"Connection error: "+error);
            if(error==null)addBubble("HOLYDON AI",result,false);
        });
    }
    private void showCode(){
        intro("Code assistant","Explain bugs, write code, review projects and learn step by step.");
        composer=new EditText(this); composer.setMinLines(4); composer.setGravity(Gravity.TOP); composer.setHint("Describe what you want to code…");
        composer.setTextColor(Color.WHITE); composer.setHintTextColor(c("#77839B")); composer.setBackground(bg(c("#151D2D"),18)); content.addView(composer);
        Button go=button("ASK HOLYDON"); content.addView(go); go.setOnClickListener(v->sendChat(true));
    }
    private void showImage(){
        intro("Image Studio","Describe a new image or choose a picture to edit.");
        content.addView(label("Prompt",13,c("#9BA8C2")));
        composer=new EditText(this); composer.setMinLines(3); composer.setGravity(Gravity.TOP); composer.setHint("Add a cinematic background, remove an object, change the scene…");
        composer.setTextColor(Color.WHITE); composer.setHintTextColor(c("#77839B")); composer.setBackground(bg(c("#151D2D"),18)); content.addView(composer);
        Button pick=button("CHOOSE IMAGE TO EDIT"); content.addView(pick);
        TextView chosen=label("No image selected",12,c("#AAB6CA")); content.addView(chosen);
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,42);});
        Button gen=button("GENERATE / EDIT IMAGE"); content.addView(gen);
        gen.setOnClickListener(v->{
            if(composer.getText().toString().trim().isEmpty()){chosen.setText("Enter an image instruction first.");return;}
            chosen.setText("Image provider setup is required on the backend. Groq is a text model and does not generate images.");
        });
        content.addView(label("Image generation/editing needs a separate image provider. The app will not charge you or silently subscribe you.",12,c("#F4C778")));
    }
    private void showPdf(){
        intro("PDF intelligence","Choose a PDF and ask HOLYDON AI to extract and explain its contents.");
        Button pick=button("SELECT PDF"); content.addView(pick);
        TextView file=label("No document selected",12,c("#AAB6CA")); content.addView(file);
        composer=new EditText(this); composer.setHint("What should I find or explain in the PDF?"); composer.setTextColor(Color.WHITE); composer.setHintTextColor(c("#77839B")); composer.setBackground(bg(c("#151D2D"),18)); content.addView(composer);
        pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("application/pdf");startActivityForResult(i,43);});
        Button ask=button("ANALYSE PDF"); content.addView(ask);
        ask.setOnClickListener(v->{if(selectedFile==null){file.setText("Select a PDF first.");return;} file.setText("PDF selected. Backend upload support is being connected.");});
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data); if(res==RESULT_OK&&data!=null){selectedFile=data.getData(); Toast.makeText(this,"File selected",Toast.LENGTH_SHORT).show();}
    }
    interface Callback{void done(String result,String error);}
    private void request(String path,JSONObject body,Callback cb){
        worker.execute(()->{
            HttpURLConnection conn=null; String answer=null,err=null;
            try{
                URL url=new URL(API_BASE+path); conn=(HttpURLConnection)url.openConnection(); conn.setRequestMethod("POST");
                conn.setConnectTimeout(20000);conn.setReadTimeout(60000);conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");
                try(OutputStream os=conn.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                InputStream in=(conn.getResponseCode()<400)?conn.getInputStream():conn.getErrorStream();
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
                JSONObject json=new JSONObject(out.toString("UTF-8"));
                if(conn.getResponseCode()>=400)throw new Exception(json.optString("error","HTTP "+conn.getResponseCode()));
                answer=json.optString("reply","No response returned.");
            }catch(Exception e){err=e.getMessage();}
            finally{if(conn!=null)conn.disconnect();}
            final String a=answer,er=err;runOnUiThread(()->cb.done(a,er));
        });
    }
    @Override protected void onDestroy(){super.onDestroy();worker.shutdownNow();}
}
