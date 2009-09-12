package edu.ucla.cens.whatsnoisy.services;

import edu.ucla.cens.whatsnoisy.services.ILocationServiceCallback;

interface ILocationService{
	void registerCallback(ILocationServiceCallback cb);
	void unregisterCallback(ILocationServiceCallback cb);
}