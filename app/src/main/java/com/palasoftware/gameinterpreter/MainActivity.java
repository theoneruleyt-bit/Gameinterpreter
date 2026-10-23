package com.palasoftware.gameinterpreter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
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

  @Override public void onCreate(Bundle state){super.onCreate(state);
    getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(22,24,22,16);root.setBackgroundColor(BG);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);
    TextView title=text("GAMEINTERPRETER",26,ACCENT);title.setGravity(Gravity.CENTER);root.addView(title);
    TextView sub=text("CONSOLE LANGUAGE COMPANION  •  V0.1",11,0xFF9CA8C2);sub.setGravity(Gravity.CENTER);root.addView(sub);
    status=text("Çeviri modeli hazırlanıyor…",14,0xFFFFCF70);root.addView(status);
    LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);root.addView(buttons);
    Button camera=button("📷 KAMERA");Button listen=button("🎧 DİNLE");Button stop=button("■ DURDUR");
    buttons.addView(camera,new LinearLayout.LayoutParams(0,56,1));buttons.addView(listen,new LinearLayout.LayoutParams(0,56,1));root.addView(stop);
    speakSwitch=new Switch(this);speakSwitch.setText("Türkçe sesli oku");speakSwitch.setTextColor(FG);root.addView(speakSwitch);
    preview=new PreviewView(this);preview.setVisibility(View.GONE);root.addView(preview,new LinearLayout.LayoutParams(-1,280));
    root.addView(text("ALGILANAN İNGİLİZCE",12,ACCENT));original=text("Henüz metin algılanmadı.",18,FG);root.addView(original);
    root.addView(text("TÜRKÇE ÇEVİRİ",12,ACCENT));translated=text("Çeviri burada görünecek.",21,FG);root.addView(translated);
    root.addView(text("İlk kullanımda çeviri dil paketi internet üzerinden indirilir. Dinleme modu Android konuşma tanıma servisine bağlıdır. Kulaklık ve televizyon ses düzeyi sonuçları etkiler.",12,0xFF9CA8C2));
    camera.setOnClickListener(v->requestMode(1));listen.setOnClickListener(v->requestMode(2));stop.setOnClickListener(v->stopAll());
    tts=new TextToSpeech(this,result->{if(result==TextToSpeech.SUCCESS){tts.setLanguage(new Locale("tr","TR"));}});
    translator=Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.TURKISH).build());
    translator.downloadModelIfNeeded(new DownloadConditions.Builder().build()).addOnSuccessListener(x->{modelReady=true;status.setText("Çeviri hazır • Kamera veya Dinle seçin");}).addOnFailureListener(e->status.setText("Dil paketi indirilemedi: "+e.getMessage()));
  }
  private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(4,14,4,14);return t;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  private void requestMode(int requested){if(!modelReady){status.setText("Önce dil paketinin inmesini bekleyin");return;}String permission=requested==1?Manifest.permission.CAMERA:Manifest.permission.RECORD_AUDIO;
    if(ContextCompat.checkSelfPermission(this,permission)!=PackageManager.PERMISSION_GRANTED){mode=requested;requestPermissions(new String[]{permission},REQUEST_PERMS);return;}startMode(requested);}
  @Override public void onRequestPermissionsResult(int code,@NonNull String[] permissions,@NonNull int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==REQUEST_PERMS&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startMode(mode);else status.setText("İzin verilmedi. Android ayarlarından izin açın.");}
  private void startMode(int requested){stopAll();mode=requested;if(requested==1)startCamera();else startListen();}
  private void translate(String input,boolean fromVoice){if(!modelReady||input.trim().isEmpty())return;original.setText(input);translator.translate(input).addOnSuccessListener(output->{if(isFinishing()||isDestroyed())return;translated.setText(output);status.setText("Çeviri güncellendi");if(speakSwitch.isChecked()&&tts!=null){if(fromVoice){voiceBusy=true;listening=false;if(speech!=null)speech.cancel();}tts.speak(output,TextToSpeech.QUEUE_FLUSH,null,"translation");if(fromVoice)handler.postDelayed(()->{voiceBusy=false;if(mode==2)restartListen();},Math.max(1600,output.length()*90L));}}).addOnFailureListener(e->status.setText("Çeviri hatası: "+e.getMessage()));}
  private void startCamera(){cameraActive=true;preview.setVisibility(View.VISIBLE);status.setText("Kamera başlatılıyor…");ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);
    f.addListener(()->{try{ProcessCameraProvider provider=f.get();if(!cameraActive)return;provider.unbindAll();Preview pv=new Preview.Builder().build();pv.setSurfaceProvider(preview.getSurfaceProvider());ImageAnalysis analysis=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();analysis.setAnalyzer(cameraExecutor,image->{long now=System.currentTimeMillis();if(!cameraActive||processing||now-lastFrame<1100){image.close();return;}lastFrame=now;processing=true;try{android.media.Image frame=image.getImage();if(frame==null){processing=false;image.close();return;}InputImage input=InputImage.fromMediaImage(frame,image.getImageInfo().getRotationDegrees());ocr.process(input).addOnSuccessListener(result->{String raw=result.getText().trim();if(cameraActive&&!raw.isEmpty()&&!raw.equals(lastCamera)){lastCamera=raw;translate(raw,false);}}).addOnCompleteListener(task->{processing=false;image.close();});}catch(Exception ex){processing=false;image.close();}});provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pv,analysis);status.setText("Kamera açık • Ekrandaki İngilizce yazıya doğrultun");}catch(Exception e){status.setText("Kamera hatası: "+e.getMessage());}},ContextCompat.getMainExecutor(this));}
  private void startListen(){if(!SpeechRecognizer.isRecognitionAvailable(this)){status.setText("Bu cihazda konuşma tanıma servisi bulunamadı");return;}speech=SpeechRecognizer.createSpeechRecognizer(this);speech.setRecognitionListener(new RecognitionListener(){
    public void onReadyForSpeech(Bundle b){status.setText("Dinleniyor…");}public void onBeginningOfSpeech(){}public void onRmsChanged(float x){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}
    public void onError(int e){listening=false;if(mode==2&&!voiceBusy)handler.postDelayed(()->restartListen(),e==SpeechRecognizer.ERROR_RECOGNIZER_BUSY?1400:700);}
    public void onResults(Bundle b){listening=false;ArrayList<String> items=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty()){String s=items.get(0);if(!s.equals(lastSpeech)){lastSpeech=s;translate(s,true);}}if(mode==2&&!voiceBusy)handler.postDelayed(()->restartListen(),800);}
    public void onPartialResults(Bundle b){ArrayList<String> items=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(items!=null&&!items.isEmpty())original.setText(items.get(0));}
    public void onEvent(int event,Bundle b){}
  });restartListen();}
  private void restartListen(){if(mode!=2||voiceBusy||speech==null||listening)return;Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"en-US");i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);try{listening=true;speech.startListening(i);}catch(Exception e){listening=false;status.setText("Mikrofon hatası: "+e.getMessage());}}
  private void stopAll(){mode=0;cameraActive=false;listening=false;voiceBusy=false;preview.setVisibility(View.GONE);if(speech!=null){speech.cancel();speech.destroy();speech=null;}if(tts!=null)tts.stop();status.setText(modelReady?"Durduruldu":"Model hazırlanıyor");}
  @Override protected void onDestroy(){stopAll();cameraExecutor.shutdown();ocr.close();if(translator!=null)translator.close();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
