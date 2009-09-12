package edu.ucla.cens.whatsnoisy.tools;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.media.MediaRecorder;

public class AudioRecorder {

  final MediaRecorder recorder = new MediaRecorder();
  final String path;
  final String SAMPLE_DATA_PATH = "/sdcard/whatsnoisy";

  public AudioRecorder() {
	Date now = new Date(); 
    this.path = SAMPLE_DATA_PATH + "/" + now.getTime() + ".amr";
  }

  public void start() throws IOException {
    String state = android.os.Environment.getExternalStorageState();
    if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
        throw new IOException("SD Card is not mounted.  It is " + state + ".");
    }

    // make sure the directory we plan to store the recording in exists
    File directory = new File(path).getParentFile();
    if (!directory.exists() && !directory.mkdirs()) {
      throw new IOException("Path to file could not be created.");
    }

    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    recorder.setOutputFile(path);
    recorder.prepare();
    recorder.start();
  }

  public void stop() throws IOException {
    recorder.stop();
    recorder.release();
  }
  
  public String getPath() {
	  return path;
  }

}
