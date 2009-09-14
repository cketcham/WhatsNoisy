package edu.ucla.cens.whatsnoisy.tools;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import android.media.MediaRecorder;

public class AudioRecorder {

	final MediaRecorder recorder = new MediaRecorder();
	String path;
	final String SAMPLE_DATA_PATH = "/sdcard/whatsnoisy";
	private Boolean recording;

	public AudioRecorder() {
		recording = false;
	}

	public void start() throws IOException {
		Date now = new Date(); 
		String state = android.os.Environment.getExternalStorageState();
		if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
			throw new IOException("SD Card is not mounted.  It is " + state + ".");
		}
		
		path = SAMPLE_DATA_PATH + "/" + now.getTime() + ".amr";

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
		recording = true;
	}

	public void stop() throws IOException {
		recorder.stop();
		recorder.release();
		recording = false;
	}
	
	public String getPath() {
		return path;
	}

	public Boolean isRecording() {
		return recording;
	}

}
