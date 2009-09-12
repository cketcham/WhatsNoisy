package edu.ucla.cens.whatsnoisy.services;

import android.location.Location;

interface ILocationServiceCallback{
	void locationUpdated(in Location l);
}