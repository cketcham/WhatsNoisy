package edu.ucla.cens.whatsnoisy.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class PCMRecorder {

	private static final String TAG = "PCMRecorder";
	AudioRecord audioRecord;
	DataOutputStream dos;
	String path;
	final String SAMPLE_DATA_PATH = "/sdcard/whatsnoisy";
	private Boolean recording;

	public PCMRecorder() {
		recording = false;
	}


	public void play() {
		// Get the file we want to playback.
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/whatsnoisy/reverseme.pcm");
		// Get the length of the audio stored in the file (16 bit so 2 bytes per short)
		// and create a short array to store the recorded audio.
		int musicLength = (int)(file.length()/2);
		short[] music = new short[musicLength];


		try {
			// Create a DataInputStream to read the audio data back from the saved file.
			InputStream is = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(is);
			DataInputStream dis = new DataInputStream(bis);

			// Read the file into the music array.
			int i = 0;
			while (dis.available() > 0) {
				music[musicLength-1-i] = dis.readShort();
				i++;
			}


			// Close the input streams.
			dis.close();    


			// Create a new AudioTrack object using the same parameters as the AudioRecord
			// object used to create the file.
			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					11025,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					musicLength,
					AudioTrack.MODE_STREAM);
			// Start playback
			audioTrack.play();

			// Write the music buffer to the AudioTrack object
			audioTrack.write(music, 0, musicLength);


		} catch (Throwable t) {
			Log.e("AudioTrack","Playback Failed");
		}
	}

	public void start() {
		  Thread thread = new Thread(new Runnable() {
			    public void run() {
			      record();
			    }     
			  });
			  thread.start();
	}
	
	public void record() {
		int frequency = 11025;
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/whatsnoisy/reverseme.pcm");

		// Delete any previous recording.
		if (file.exists())
			file.delete();


		// Create the new file.
		try {
			file.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create " + file.toString());
		}

		try {
			// Create a DataOuputStream to write the audio data into the saved file.
			OutputStream os = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(os);
			dos = new DataOutputStream(bos);

			// Create a new AudioRecord object to record the audio.
			int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration,  audioEncoding);
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
					frequency, channelConfiguration,
					audioEncoding, bufferSize);

			short[] buffer = new short[bufferSize];   
			audioRecord.startRecording();
			recording = true;
			Log.d(TAG, "RECORDING");

			while (isRecording()) {
				int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
				for (int i = 0; i < bufferReadResult; i++)
					dos.writeShort(buffer[i]);
			}

		} catch (Throwable t) {
			Log.e("AudioRecord","Recording Failed");
		}
	}

	public void stop() {
		Log.d(TAG, "STOP");
		audioRecord.stop();
		try {
			dos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getPath() {
		return path;
	}

	public Boolean isRecording() {
		return recording;
	}

}
