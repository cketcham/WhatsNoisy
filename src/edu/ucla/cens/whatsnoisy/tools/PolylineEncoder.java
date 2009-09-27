/**
 * Reimplementation of Mark McClures Javascript PolylineEncoder
 * All the mathematical logic is more or less copied by McClure
 *  
 * @author Mark Rambow
 * @e-mail markrambow[at]gmail[dot]com
 * @version 0.1
 * 
 */

package edu.ucla.cens.whatsnoisy.tools;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.StringTokenizer;

import edu.ucla.cens.whatsnoisy.data.LocationDatabase.LocationRow;


public class PolylineEncoder {

	private int numLevels = 18;

	private int zoomFactor = 2;

	private double verySmall = 0.00001;

	private boolean forceEndpoints = true;

	private double[] zoomLevelBreaks;

	private HashMap<String, Double> bounds;

	// constructor
	public PolylineEncoder(int numLevels, int zoomFactor, double verySmall,
			boolean forceEndpoints) {

		this.numLevels = numLevels;
		this.zoomFactor = zoomFactor;
		this.verySmall = verySmall;
		this.forceEndpoints = forceEndpoints;

		this.zoomLevelBreaks = new double[numLevels];

		for (int i = 0; i < numLevels; i++) {
			this.zoomLevelBreaks[i] = verySmall
					* Math.pow(this.zoomFactor, numLevels - i - 1);
		}
	}

	public PolylineEncoder() {
		this.zoomLevelBreaks = new double[numLevels];

		for (int i = 0; i < numLevels; i++) {
			this.zoomLevelBreaks[i] = verySmall
					* Math.pow(this.zoomFactor, numLevels - i - 1);
		}
	}

	/**
	 * Douglas-Peucker algorithm, adapted for encoding
	 * 
	 * @return HashMap [EncodedPoints;EncodedLevels]
	 * 
	 */
	public HashMap<String, String> dpEncode(Track track) {
		int i, maxLoc = 0;
		Stack<int[]> stack = new Stack<int[]>();
		double[] dists = new double[track.getLocationRows().size()];
		double maxDist, absMaxDist = 0.0, temp = 0.0;
		int[] current;
		String encodedPoints, encodedLevels;

		if (track.getLocationRows().size() > 2) {
			int[] stackVal = new int[] { 0, (track.getLocationRows().size() - 1) };
			stack.push(stackVal);

			while (stack.size() > 0) {
				current = stack.pop();
				maxDist = 0;

				for (i = current[0] + 1; i < current[1]; i++) {
					temp = this.distance(track.getLocationRows().get(i), track
							.getLocationRows().get(current[0]), track
							.getLocationRows().get(current[1]));
					if (temp > maxDist) {
						maxDist = temp;
						maxLoc = i;
						if (maxDist > absMaxDist) {
							absMaxDist = maxDist;
						}
					}
				}
				if (maxDist > this.verySmall) {
					dists[maxLoc] = maxDist;
					int[] stackValCurMax = { current[0], maxLoc };
					stack.push(stackValCurMax);
					int[] stackValMaxCur = { maxLoc, current[1] };
					stack.push(stackValMaxCur);
				}
			}
		}

		// System.out.println("createEncodings(" + track.getLocationRows().size()
		// + "," + dists.length + ")");
		encodedPoints = createEncodings(track.getLocationRows(), dists);
		// System.out.println("encodedPoints \t\t: " + encodedPoints);
		// encodedPoints.replace("\\","\\\\");
		encodedPoints = replace(encodedPoints, "\\", "\\\\");
		//System.out.println("encodedPoints slashy?\t\t: " + encodedPoints);

		encodedLevels = encodeLevels(track.getLocationRows(), dists, absMaxDist);
		//System.out.println("encodedLevels: " + encodedLevels);

		HashMap<String, String> hm = new HashMap<String, String>();
		hm.put("encodedPoints", encodedPoints);
		hm.put("encodedLevels", encodedLevels);
		return hm;

	}

	public String replace(String s, String one, String another) {
		// In a string replace one substring with another
		if (s.equals(""))
			return "";
		String res = "";
		int i = s.indexOf(one, 0);
		int lastpos = 0;
		while (i != -1) {
			res += s.substring(lastpos, i) + another;
			lastpos = i + one.length();
			i = s.indexOf(one, lastpos);
		}
		res += s.substring(lastpos); // the rest
		return res;
	}

	/**
	 * distance(p0, p1, p2) computes the distance between the point p0 and the
	 * segment [p1,p2]. This could probably be replaced with something that is a
	 * bit more numerically stable.
	 * 
	 * @param p0
	 * @param p1
	 * @param p2
	 * @return
	 */
	public double distance(LocationRow p0, LocationRow p1, LocationRow p2) {
		double u, out = 0.0;

		if (p1.location.getLatitude() == p2.location.getLatitude()
				&& p1.location.getLongitude() == p2.location.getLongitude()) {
			out = Math.sqrt(Math.pow(p2.location.getLatitude() - p0.location.getLatitude(), 2)
					+ Math.pow(p2.location.getLongitude() - p0.location.getLongitude(), 2));
		} else {
			u = ((p0.location.getLatitude() - p1.location.getLatitude())
					* (p2.location.getLatitude() - p1.location.getLatitude()) + (p0
					.location.getLongitude() - p1.location.getLongitude())
					* (p2.location.getLongitude() - p1.location.getLongitude()))
					/ (Math.pow(p2.location.getLatitude() - p1.location.getLatitude(), 2) + Math
							.pow(p2.location.getLongitude() - p1.location.getLongitude(), 2));

			if (u <= 0) {
				out = Math.sqrt(Math.pow(p0.location.getLatitude() - p1.location.getLatitude(),
						2)
						+ Math.pow(p0.location.getLongitude() - p1.location.getLongitude(), 2));
			}
			if (u >= 1) {
				out = Math.sqrt(Math.pow(p0.location.getLatitude() - p2.location.getLatitude(),
						2)
						+ Math.pow(p0.location.getLongitude() - p2.location.getLongitude(), 2));
			}
			if (0 < u && u < 1) {
				out = Math.sqrt(Math.pow(p0.location.getLatitude() - p1.location.getLatitude()
						- u * (p2.location.getLatitude() - p1.location.getLatitude()), 2)
						+ Math.pow(p0.location.getLongitude() - p1.location.getLongitude() - u
								* (p2.location.getLongitude() - p1.location.getLongitude()), 2));
			}
		}
		return out;
	}


	private static int floor1e5(double coordinate) {
		return (int) Math.floor(coordinate * 1e5);
	}

	private static String encodeSignedNumber(int num) {
		int sgn_num = num << 1;
		if (num < 0) {
			sgn_num = ~(sgn_num);
		}
		return (encodeNumber(sgn_num));
	}

	private static String encodeNumber(int num) {

		StringBuffer encodeString = new StringBuffer();

		while (num >= 0x20) {
			int nextValue = (0x20 | (num & 0x1f)) + 63;
			encodeString.append((char) (nextValue));
			num >>= 5;
		}

		num += 63;
		encodeString.append((char) (num));

		return encodeString.toString();
	}

	/**
	 * Now we can use the previous function to march down the list of points and
	 * encode the levels. Like createEncodings, we ignore points whose distance
	 * (in dists) is undefined.
	 */
	private String encodeLevels(ArrayList<LocationRow> points, double[] dists,
			double absMaxDist) {
		int i;
		StringBuffer encoded_levels = new StringBuffer();

		if (this.forceEndpoints) {
			encoded_levels.append(encodeNumber(this.numLevels - 1));
		} else {
			encoded_levels.append(encodeNumber(this.numLevels
					- computeLevel(absMaxDist) - 1));
		}
		for (i = 1; i < points.size() - 1; i++) {
			if (dists[i] != 0) {
				encoded_levels.append(encodeNumber(this.numLevels
						- computeLevel(dists[i]) - 1));
			}
		}
		if (this.forceEndpoints) {
			encoded_levels.append(encodeNumber(this.numLevels - 1));
		} else {
			encoded_levels.append(encodeNumber(this.numLevels
					- computeLevel(absMaxDist) - 1));
		}
//		System.out.println("encodedLevels: " + encoded_levels);
		return encoded_levels.toString();
	}

	/**
	 * This computes the appropriate zoom level of a point in terms of it's
	 * distance from the relevant segment in the DP algorithm. Could be done in
	 * terms of a logarithm, but this approach makes it a bit easier to ensure
	 * that the level is not too large.
	 */
	private int computeLevel(double absMaxDist) {
		int lev = 0;
		if (absMaxDist > this.verySmall) {
			lev = 0;
			while (absMaxDist < this.zoomLevelBreaks[lev]) {
				lev++;
			}
			return lev;
		}
		return lev;
	}

	private String createEncodings(ArrayList<LocationRow> points, double[] dists) {
		StringBuffer encodedPoints = new StringBuffer();

		double maxlat = 0, minlat = 0, maxlon = 0, minlon = 0;

		int plat = 0;
		int plng = 0;
		
		for (int i = 0; i < points.size(); i++) {

			// determin bounds (max/min lat/lon)
			if (i == 0) {
				maxlat = minlat = points.get(i).location.getLatitude();
				maxlon = minlon = points.get(i).location.getLongitude();
			} else {
				if (points.get(i).location.getLatitude() > maxlat) {
					maxlat = points.get(i).location.getLatitude();
				} else if (points.get(i).location.getLatitude() < minlat) {
					minlat = points.get(i).location.getLatitude();
				} else if (points.get(i).location.getLongitude() > maxlon) {
					maxlon = points.get(i).location.getLongitude();
				} else if (points.get(i).location.getLongitude() < minlon) {
					minlon = points.get(i).location.getLongitude();
				}
			}

			if (dists[i] != 0 || i == 0 || i == points.size() - 1) {
				LocationRow point = points.get(i);

				int late5 = floor1e5(point.location.getLatitude());
				int lnge5 = floor1e5(point.location.getLongitude());

				int dlat = late5 - plat;
				int dlng = lnge5 - plng;

				plat = late5;
				plng = lnge5;

				encodedPoints.append(encodeSignedNumber(dlat));
				encodedPoints.append(encodeSignedNumber(dlng));

			}
		}

		HashMap<String, Double> bounds = new HashMap<String, Double>();
		bounds.put("maxlat", new Double(maxlat));
		bounds.put("minlat", new Double(minlat));
		bounds.put("maxlon", new Double(maxlon));
		bounds.put("minlon", new Double(minlon));

		this.setBounds(bounds);
		return encodedPoints.toString();
	}

	private void setBounds(HashMap<String, Double> bounds) {
		this.bounds = bounds;
	}

	public static HashMap createEncodings(Track track, int level, int step) {

		HashMap<String, String> resultMap = new HashMap<String, String>();
		StringBuffer encodedPoints = new StringBuffer();
		StringBuffer encodedLevels = new StringBuffer();

		ArrayList LocationRowList = (ArrayList) track.getLocationRows();

		int plat = 0;
		int plng = 0;
		int counter = 0;

		int listSize = LocationRowList.size();

		LocationRow LocationRow;

		for (int i = 0; i < listSize; i += step) {
			counter++;
			LocationRow = (LocationRow) LocationRowList.get(i);

			int late5 = floor1e5(LocationRow.location.getLatitude());
			int lnge5 = floor1e5(LocationRow.location.getLongitude());

			int dlat = late5 - plat;
			int dlng = lnge5 - plng;

			plat = late5;
			plng = lnge5;

			encodedPoints.append(encodeSignedNumber(dlat)).append(
					encodeSignedNumber(dlng));
			encodedLevels.append(encodeNumber(level));

		}

		System.out.println("listSize: " + listSize + " step: " + step
				+ " counter: " + counter);

		resultMap.put("encodedPoints", encodedPoints.toString());
		resultMap.put("encodedLevels", encodedLevels.toString());

		return resultMap;
	}

	public HashMap<String, Double> getBounds() {
		return bounds;
	}
	
	public int getNumLevels() {
		return numLevels;
	}

	public int getZoomFactor() { 
		return zoomFactor;
	}
}
