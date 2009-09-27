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

import edu.ucla.cens.whatsnoisy.data.LocationDatabase.LocationRow;

public class Track {

    private ArrayList<LocationRow> locationrows = new ArrayList<LocationRow>();
    
    public Track() {
    	
    }

    public Track(ArrayList<LocationRow> locationrows) {
    	this.locationrows = locationrows;
    }
    
    public ArrayList<LocationRow> getLocationRows() {
        return this.locationrows;
    }

    public void setLocationRows(ArrayList<LocationRow> locationrows) {
        this.locationrows = locationrows;
    }

    public void addLocationRow(LocationRow trkpt) {
		this.locationrows.add(trkpt);
	}

}
