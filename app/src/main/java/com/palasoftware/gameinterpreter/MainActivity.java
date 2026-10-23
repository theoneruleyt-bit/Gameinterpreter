package com.palasoftware.gameinterpreter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.SharedPreferences;
import com.android.billingclient.api.*;
import java.util.Arrays;
import java.util.List;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
  private static final int REQUEST_PERMS=33;
  private static final long TRIAL_MS=180000L;
  private static final String PREFS="gameinterpreter_trial_v1";
  private BillingClient billing;
  private boolean premium=false;
  private int translateGeneration=0;
  private int listenFailures=0;
  private String lastTranslated="";
  private long lastSpeechAt=0;
  private final java.util.ArrayDeque<String> recentLines=new java.util.ArrayDeque<>();
  private TextView historyLabel;
  private long trialDeadline=0;
  private boolean trialStarted=false;
  private TextView licenseLabel;
  private final Runnable trialGuard=new Runnable(){@Override public void run(){
    if(!premium && trialStarted && trialDeadline>0 && android.os.SystemClock.elapsedRealtime()>=trialDeadline){stopAll();if(licenseLabel!=null)licenseLabel.setText("Deneme süresi doldu • Premium gerekli");}
    else if(licenseLabel!=null&&!premium){long remaining=trialStarted?Math.max(0,(trialDeadline-android.os.SystemClock.elapsedRealtime()+999)/1000):180;licenseLabel.setText("Ücretsiz deneme: "+(remaining/60)+":"+String.format(java.util.Locale.US,"%02d",remaining%60));}
    handler.postDelayed(this,1000);
  }};

  private final Handler handler=new Handler(Looper.getMainLooper());
  private final ExecutorService cameraExecutor=Executors.newSingleThreadExecutor();
  private final com.google.mlkit.vision.text.TextRecognizer ocr=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
  private Translator translator;
  private TextToSpeech tts;
  private SpeechRecognizer speech;
  private PreviewView preview;
  private TextView status,original,translated;
  private Switch speakSwitch;
  private boolean modelReady=false,listening=false,cameraActive=false,processing=false,voiceBusy=false;
  private String lastCamera="",lastSpeech="";
  private long lastFrame=0;
  private int mode=0;
  private final int BG=Color.rgb(11,16,32),FG=Color.WHITE,ACCENT=Color.rgb(37,217,209);

  private int dp(float value){return (int)(getResources().getDisplayMetrics().density*value+0.5f);}
  private GradientDrawable shape(int fill,int stroke,int radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
  private TextView label(String value,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private void addGap(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
  private LinearLayout card(LinearLayout root,int color,int border){LinearLayout l=vertical();l.setPadding(dp(17),dp(17),dp(17),dp(17));l.setBackground(shape(color,border,18));root.addView(l,new LinearLayout.LayoutParams(-1,-2));return l;}
  private TextView text(String s,int size,int color){TextView t=label(s,size,color,false);t.setPadding(0,dp(8),0,dp(8));return t;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackgroundTintList(null);return b;}
  @Override public void onCreate(Bundle state){super.onCreate(state);
    getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);scroll.setClipToPadding(false);
    LinearLayout root=vertical();root.setPadding(dp(18),dp(22),dp(18),dp(28));scroll.addView(root);setContentView(scroll);
    LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading);
    TextView emblem=label("✦",25,ACCENT,true);emblem.setGravity(Gravity.CENTER);emblem.setBackground(shape(0xFF153940,0,14));heading.addView(emblem,new LinearLayout.LayoutParams(dp(52),dp(52)));
    LinearLayout titles=vertical();titles.setPadding(dp(12),0,0,0);heading.addView(titles);
    titles.addView(label("GAMEINTERPRETER",19,Color.WHITE,true));titles.addView(label("CONSOLE LANGUAGE COMPANION  ·  V0.4",10,0xFF9DAEC8,false));
    addGap(root,22);
    LinearLayout ready=card(root,0xFF102C31,0xFF235A59);status=label("Çeviri modeli hazırlanıyor…",15,0xFFB8F5E8,true);ready.addView(status);
    addGap(root,20);root.addView(label("ÇEVİRİ MODUNU SEÇ",12,0xFF99AEC5,true));addGap(root,10);
    LinearLayout modes=new LinearLayout(this);root.addView(modes);
    Button camera=button("▣  Kamera");Button listen=button("◉  Canlı Dinle");
    camera.setBackground(shape(0xFF15545C,0xFF298C95,17));listen.setBackground(shape(0xFF39275B,0xFF7256A3,17));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(70),1);cp.setMargins(0,0,dp(6),0);modes.addView(camera,cp);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(70),1);lp.setMargins(dp(6),0,0,0);modes.addView(listen,lp);
    addGap(root,14);
    LinearLayout session=card(root,0xFF151F34,0xFF2A3B54);session.addView(label("CANLI OTURUM",12,0xFF77DFD7,true));addGap(session,10);
    Button stop=button("■  Çeviriyi Durdur");stop.setBackground(shape(0xFF303A51,0,12));session.addView(stop,new LinearLayout.LayoutParams(-1,dp(50)));
    addGap(root,14);
    preview=new PreviewView(this);preview.setVisibility(View.GONE);preview.setBackground(shape(0xFF0F1929,0,16));root.addView(preview,new LinearLayout.LayoutParams(-1,dp(245)));
    addGap(root,14);
    LinearLayout translation=card(root,0xFF151F34,0xFF2A3B54);
    translation.addView(label("ENGLISH  ·  ALGILANAN",12,0xFF8FA7BF,true));addGap(translation,10);
    original=label("Henüz metin algılanmadı.",18,0xFFE6ECF7,false);translation.addView(original);
    addGap(translation,18);View divider=new View(this);divider.setBackgroundColor(0xFF314057);translation.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));addGap(translation,18);
    translation.addView(label("TÜRKÇE  ·  ÇEVİRİ",12,ACCENT,true));addGap(translation,10);
    translated=label("Çeviri burada görünecek.",21,Color.WHITE,true);translation.addView(translated);
    addGap(root,14);
    LinearLayout history=card(root,0xFF151F34,0xFF2A3B54);history.addView(label("SON ÇEVİRİLER",12,ACCENT,true));addGap(history,8);historyLabel=label("Henüz çeviri yapılmadı.",13,0xFFB9C8DA,false);history.addView(historyLabel);
    addGap(root,14);
    LinearLayout audio=card(root,0xFF151F34,0xFF2A3B54);LinearLayout audioRow=new LinearLayout(this);audioRow.setGravity(Gravity.CENTER_VERTICAL);audio.addView(audioRow);
    LinearLayout audioLabels=vertical();audioRow.addView(audioLabels,new LinearLayout.LayoutParams(0,-2,1));audioLabels.addView(label("Türkçe seslendirme",15,Color.WHITE,true));audioLabels.addView(label("Çevirileri sesli oku",12,0xFF9CAFC8,false));
    speakSwitch=new Switch(this);speakSwitch.setShowText(false);audioRow.addView(speakSwitch);
    addGap(root,18);root.addView(label("İlk kullanımda çeviri modeli indirilir. Canlı Dinle modu cihazın konuşma tanıma servisini kullanır; gecikme ve tanıma doğruluğu cihaza göre değişebilir.",12,0xFF93A3BC,false));
    addGap(root,12);
    LinearLayout paywall=card(root,0xFF171D34,0xFF5E4B8D);
    paywall.addView(label("GAMEINTERPRETER PREMIUM",15,0xFFBDA4FF,true));
    licenseLabel=label("Lisans kontrol ediliyor…",14,Color.WHITE,false);paywall.addView(licenseLabel);
    paywall.addView(label("3 dakika deneme • Aylık $5.99 • Yıllık $69.99 • Ömür boyu $99.99",12,0xFFB7C5D9,false));
    Button monthly=button("Aylık abonelik");paywall.addView(monthly);
    Button yearly=button("Yıllık abonelik");paywall.addView(yearly);
    Button lifetime=button("Ömür boyu lisans");paywall.addView(lifetime);
    Button restore=button("Satın alımları geri yükle");paywall.addView(restore);
    monthly.setOnClickListener(v->purchase("gameinterpreter_monthly",BillingClient.ProductType.SUBS));
    yearly.setOnClickListener(v->purchase("gameinterpreter_yearly",BillingClient.ProductType.SUBS));
    lifetime.setOnClickListener(v->purchase("gameinterpreter_lifetime",BillingClient.ProductType.INAPP));
    restore.setOnClickListener(v->refreshPurchases());
    SharedPreferences prefs=getSharedPreferences(PREFS,MODE_PRIVATE);
    trialStarted=prefs.getBoolean("started",false);
    // A local timer is ONLY a demo gate. Production requires an authenticated server trial.
    if(trialStarted){trialDeadline=0;}
    handler.post(trialGuard);
    initBilling();
    camera.setOnClickListener(v->requestMode(1));listen.setOnClickListener(v->requestMode(2));stop.setOnClickListener(v->stopAll());
    tts=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("tr","TR"));}});
    translator=Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.TURKISH).build());
    translator.downloadModelIfNeeded(new DownloadConditions.Builder().build()).addOnSuccessListener(x->{modelReady=true;status.setText("●  Tercüman hazır · Mod seçin");}).addOnFailureListener(e->status.setText("Dil paketi indirilemedi: "+e.getMessage()));
  }
  private boolean hasAccess(){return premium || (!trialStarted || (trialDeadline>0 && android.os.SystemClock.elapsedRealtime()<trialDeadline));}
  private void initBilling(){
    billing=BillingClient.newBuilder(this).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
      .setListener((result,purchases)->{if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && purchases!=null){
        // Production: send tokens to authenticated backend and unlock ONLY after server verification.
        status.setText("Satın alma alındı; sunucu doğrulaması gereklidir");
      }}).build();
    billing.startConnection(new BillingClientStateListener(){
      @Override public void onBillingSetupFinished(BillingResult result){if(result.getResponseCode()==BillingClient.BillingResponseCode.OK)refreshPurchases();}
      @Override public void onBillingServiceDisconnected(){status.setText("Play Billing bağlantısı kesildi");}
    });
  }
  private void refreshPurchases(){
    if(billing==null||!billing.isReady())return;
    for(String type:new String[]{BillingClient.ProductType.SUBS,BillingClient.ProductType.INAPP}){
      billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build(),(result,purchases)->{
        if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && !purchases.isEmpty())
          status.setText("Satın alma bulundu. Premium için sunucu doğrulaması gerekli.");
      });
    }
  }
  private void purchase(String id,String type){
    if(billing==null||!billing.isReady()){status.setText("Google Play ödeme servisi hazır değil");return;}
    QueryProductDetailsParams.Product product=QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build();
    billing.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Arrays.asList(product)).build(),(result,details)->{
      if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK||details==null||details.isEmpty()){
        status.setText("Ürün Play Console'da bulunamadı veya test hesabı uygun değil");return;}
      ProductDetails detail=details.get(0);
      BillingFlowParams.ProductDetailsParams.Builder pb=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(detail);
      if(BillingClient.ProductType.SUBS.equals(type)){
        if(detail.getSubscriptionOfferDetails()==null||detail.getSubscriptionOfferDetails().isEmpty()){status.setText("Abonelik teklifi tanımlanmamış");return;}
        pb.setOfferToken(detail.getSubscriptionOfferDetails().get(0).getOfferToken());
      }
      billing.launchBillingFlow(this,BillingFlowParams.newBuilder().setProductDetailsParamsList(Arrays.asList(pb.build())).build());
    });
  }
  private void requestMode(int requested){if(!hasAccess()){stopAll();status.setText("3 dakikalık deneme bitti. Premium lisans gerekli.");return;}if(!modelReady){status.setText("Önce dil paketinin inmesini bekleyin");return;}String permission=requested==1?Manifest.permission.CAMERA:Manifest.permission.RECORD_AUDIO;
    if(ContextCompat.checkSelfPermission(this,permission)!=PackageManager.PERMISSION_GRANTED){mode=requested;requestPermissions(new String[]{permission},REQUEST_PERMS);return;}startMode(requested);}
  @Override public void onRequestPermissionsResult(int code,@NonNull String[] permissions,@NonNull int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==REQUEST_PERMS&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startMode(mode);else status.setText("İzin verilmedi. Android ayarlarından izin açın.");}
  private void startMode(int requested){if(!trialStarted){trialStarted=true;trialDeadline=android.os.SystemClock.elapsedRealtime()+TRIAL_MS;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("started",true).apply();}if(!hasAccess()){status.setText("Deneme sona erdi");return;}stopAll();mode=requested;if(requested==1)startCamera();else startListen();}
  private void translate(String input,boolean fromVoice){
    if(!hasAccess()||!modelReady)return;
    String clean=input.replaceAll("\\s+"," ").trim();
    if(clean.length()<3 || clean.equalsIgnoreCase(lastTranslated))return;
    final int generation=++translateGeneration;
    original.setText(clean);
    translator.translate(clean).addOnSuccessListener(output->{
      if(isFinishing()||isDestroyed()||generation!=translateGeneration||!hasAccess())return;
      lastTranslated=clean;
      translated.setText(output);status.setText("Çeviri güncellendi");
      recentLines.addFirst("EN  "+clean+"\nTR  "+output);
      while(recentLines.size()>4)recentLines.removeLast();
      StringBuilder h=new StringBuilder();for(String line:recentLines){if(h.length()>0)h.append("\n\n");h.append(line);}
      if(historyLabel!=null)historyLabel.setText(h.toString());
      if(speakSwitch.isChecked()&&tts!=null){
        if(fromVoice){voiceBusy=true;listening=false;if(speech!=null)speech.cancel();}
        tts.speak(output,TextToSpeech.QUEUE_FLUSH,null,"translation");
        if(fromVoice)handler.postDelayed(()->{voiceBusy=false;if(mode==2)restartListen();},Math.max(1600,output.length()*90L));
      }
    }).addOnFailureListener(e->status.setText("Çeviri hatası: "+e.getMessage()));
  }
  private void startCamera(){cameraActive=true;preview.setVisibility(View.VISIBLE);status.setText("Kamera başlatılıyor…");ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
    f.addListener(()->{try{ProcessCameraProvider provider=f.get();if(!cameraActive)return;provider.unbindAll();Preview pv=new Preview.Builder().build();pv.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis analysis=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,image->{long now=System.currentTimeMillis();if(!cameraActive||processing||now-lastFrame<1100){image.close();return;}lastFrame=now;processing=true;try{android.media.Image frame=image.getImage();if(frame==null){processing=false;image.close();return;}InputImage input=InputImage.fromMediaImage(frame,image.getImageInfo().getRotationDegrees());ocr.process(input).addOnSuccessListener(result->{String raw=result.getText().trim();if(cameraActive&&!raw.isEmpty()&&!raw.equals(lastCamera)){lastCamera=raw;translate(raw,false);}}).addOnCompleteListener(task->{processing=false;image.close();});}catch(Exception ex){processing=false;image.close();}});provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pv,analysis);status.setText("Kamera açık • Ekrandaki İngilizce yazıya doğrultun");}catch(Exception e){status.setText("Kamera hatası: "+e.getMessage());}},ContextCompat.getMainExecutor(this));}
  private void startListen(){if(!SpeechRecognizer.isRecognitionAvailable(this)){status.setText("Bu cihazda konuşma tanıma servisi bulunamadı");return;}speech=SpeechRecognizer.createSpeechRecognizer(this);speech.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){status.setText("Dinleniyor…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float x){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}
    public void onError(int e){listening=false;if(mode==2&&!voiceBusy){listenFailures=Math.min(6,listenFailures+1);long delay=Math.min(8000,500L*(1<<listenFailures));handler.postDelayed(()->restartListen(),delay);}}
    public void onResults(Bundle b){listening=false;listenFailures=0;ArrayList<String> items=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty()){String s=items.get(0);if(s.trim().length()>=4&&!s.equalsIgnoreCase(lastSpeech)&&android.os.SystemClock.elapsedRealtime()-lastSpeechAt>400){lastSpeech=s;lastSpeechAt=android.os.SystemClock.elapsedRealtime();translate(s,true);}}if(mode==2&&!voiceBusy)handler.postDelayed(()->restartListen(),800);}
    public void onPartialResults(Bundle b){ArrayList<String> items=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty())original.setText(items.get(0));}
    public void onEvent(int event,Bundle b){}
  });restartListen();}
  private void restartListen(){if(!hasAccess()){stopAll();return;}if(mode!=2||voiceBusy||speech==null||listening)return;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);try{listening=true;speech.startListening(i);}catch(Exception e){listening=false;status.setText("Mikrofon hatası: "+e.getMessage());}}
  private void stopAll(){translateGeneration++;mode=0;cameraActive=false;listening=false;voiceBusy=false;preview.setVisibility(View.GONE);if(speech!=null){speech.cancel();speech.destroy();speech=null;}if(tts!=null)tts.stop();status.setText(modelReady?"Durduruldu":"Model hazırlanıyor");}
  @Override protected void onDestroy(){handler.removeCallbacks(trialGuard);if(billing!=null)billing.endConnection();stopAll();cameraExecutor.shutdown();ocr.close();if(translator!=null)translator.close();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
