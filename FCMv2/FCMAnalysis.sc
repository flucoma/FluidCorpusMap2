FCMFeatureExtractor {
	var server, <>settings, clock;
	var shapeFeatures;
	var vectorFtr;
	var tmpBufs;

	*new{|server = (Server.default),
		settings = (FCMAnalysisSettings.new),
		clock = (TempoClock.new(queueSize:8192))|
		^super.newCopyArgs(server, settings).init;
	}


	init{
		shapeFeatures = [\spectral_centroid,
		\spectral_spread, \spectral_skewness,
		\specttral_kurtosis, \spectral_rolloff,
		\spectral_flatness, \spectral_crest
		];

		vectorFtr = (
			(mfcc:FluidBufMFCC, mel:FluidBufMelBands, chroma:FluidBufChroma);
		)
	}

	getFtrUgen{|name, src, start, n, tgt, d, t = 1|
			^name.switch
		            {\mfcc} {FluidBufMFCC.kr(src, start, n, 0, -1, tgt, d, trig:t)}
			        {\mel} {FluidBufMelBands.kr(src, start, n, 0, -1, tgt, d, trig:t)}
			        {\spectralshape} {FluidBufSpectralShape.kr(src, start, n, 0, -1, tgt, trig:t)}
		            {\pitch} {FluidBufPitch.kr(src, start, n, 0, -1, tgt, trig:t)}
		            {\loudness} {FluidBufLoudness.kr(src, start, n, 0, -1, tgt, trig:t)};
	}


	nSpecFrames{|n|
		^((n + settings.hopSize) / settings.hopSize).floor;
	}

	nFrames{|n, b|
		^(n == -1).if{b.numFrames}{n}
	}

	extractPositionFeature{|src, start, numFrames, dest, t = 1|
		var nFrames = this.nFrames(numFrames, src);
		var ftrBuf = LocalBuf(this.nSpecFrames(nFrames), settings.numDims);
		var statsBuf = LocalBuf(7 * (settings.numDifs+1) , settings.numDims);
		var chain;

		chain = vectorFtr[settings.positionFtr].kr(
			src, start, nFrames, 0, -1, ftrBuf, settings.numDims, trig:t,
			windowSize:settings.windowSize, hopSize:settings.hopSize, fftSize:settings.fftSize
		);
		chain = FluidBufStats.kr(
			ftrBuf, 0, -1, 0, -1, statsBuf,
			numDerivs:settings.numDifs, trig:Done.kr(chain)
		);
		chain = FluidBufFlatten.kr(
			statsBuf,destination:dest, trig:Done.kr(chain)
		 );
		^chain;
	}

	getFtrChannel{|name, shape, pitch, loudness|
		^switch(name,
			\pitch, {[pitch ,0]},
			\pitch_salience, {[pitch, 1]},
			\loudness, {[loudness, 0]},
			\spectral_centroid, {[shape, 0]},
			\spectral_spread, {[shape , 1]},
			\spectral_flatness,{[shape , 5]}
		)
	}

	computeShape{
		^(shapeFeatures.includes(settings.shapeFtr) ||
		  shapeFeatures.includes(settings.colorFtr)
		 )
	}

	computePitch{
		^([\pitch, \pitch_salience].includes(settings.shapeFtr) ||
		  [\pitch, \pitch_salience].includes(settings.colorFtr)
		 )
	}

	computeLoudness{
		^[settings.shapeFtr, settings.colorFtr].includes(\loudness)
	}


	extractIconSeries{|shapeBuf, pitchBuf, loudBuf, destBuf, t = 1|

	}

	copyIconFeatures{|shapeBuf, pitchBuf, loudBuf, destBuf, numFrames = -1,
		dest2StartChan = 1, dest2StartFrame = 0,  chain = 1|
		var f1Buf, f1Chan, f2Buf, f2Chan;
		#f1Buf, f1Chan = this.getFtrChannel(
			settings.shapeFtr, shapeBuf, pitchBuf, loudBuf
		);
		#f2Buf, f2Chan = this.getFtrChannel(
			settings.colorFtr, shapeBuf, pitchBuf, loudBuf
		);

		chain = FluidBufCompose.kr(
			f1Buf, startChan:f1Chan, numFrames:numFrames,
			numChans:1, destination:destBuf, trig:Done.kr(chain)
		);
		chain = FluidBufCompose.kr(
			f2Buf, startChan:f2Chan, numFrames:numFrames,
			numChans:1, destination:destBuf,
			destStartFrame: dest2StartFrame,
			destStartChan:dest2StartChan, trig:Done.kr(chain)
		);
		^chain;
	}

	extractIconFeatures{|src, start, numFrames, seriesBuf, statsBuf, chain = 1|
		var nFrames = this.nFrames(numFrames, src);
		var specFrames = this.nSpecFrames(nFrames);
		var shapeBuf = LocalBuf(specFrames, 7);
		var shapeStatsBuf = LocalBuf(7, 7);
		var pitchBuf = LocalBuf(specFrames, 2);
		var pitchStatsBuf = LocalBuf(7, 2);
		var loudBuf = LocalBuf(specFrames, 2);
		var loudStatsBuf = LocalBuf(7, 2);
		var f1Buf, f1Chan, f2Buf, f2Chan;


		if (this.computeShape){
			chain = FluidBufSpectralShape.kr(
				src, start, nFrames, 0, -1, shapeBuf,
				windowSize:settings.windowSize,
				hopSize:settings.hopSize,
				fftSize:settings.fftSize
			);
			chain = FluidBufStats.kr(
				shapeBuf, stats:shapeStatsBuf, trig:Done.kr(chain)
			);
		};

		if (this.computePitch){
			chain = FluidBufPitch.kr(
				src, start, nFrames, 0, -1, pitchBuf,
				trig:Done.kr(chain),
				windowSize:settings.windowSize,
				hopSize:settings.hopSize,
				fftSize:settings.fftSize
			);
			chain = FluidBufStats.kr(
				pitchBuf, stats:pitchStatsBuf, trig:Done.kr(chain)
			);
		};

		if (this.computeLoudness){
			chain = FluidBufLoudness.kr(
				src, start, nFrames, 0, -1, loudBuf, trig:Done.kr(chain),
				//windowSize:settings.windowSize, hopSize:settings.hopSize
			);
			chain = FluidBufStats.kr(
				loudBuf, stats:loudStatsBuf, trig:Done.kr(chain)
			);
		};

		chain = this.copyIconFeatures(shapeBuf, pitchBuf, loudBuf, seriesBuf, chain:chain);
		chain = this.copyIconFeatures(shapeStatsBuf, pitchStatsBuf, loudStatsBuf,
			statsBuf, -1,0, 7, chain);

		^chain;
	}

	extract{|srcBuf, posStatsBuf, iconSeriesBuf, iconStatsBuf, start, numFrames, action|
		{
			var chain = this.extractPositionFeature(srcBuf, start, numFrames, posStatsBuf);
			chain = this.extractIconFeatures(
				srcBuf, start, numFrames, iconSeriesBuf, iconStatsBuf, chain);
			FreeSelfWhenDone.kr(chain);

		}.play(server).onFree({
			    {
				    posStatsBuf.updateInfo;
				    iconSeriesBuf.updateInfo;
				    iconStatsBuf.updateInfo;
				    server.sync;
				    action.value;
			}.forkIfNeeded(clock);
		  });
	}

	run{|srcBuf, posStatsBuf, iconFtrBuf, iconStatsBuf, start, numFrames, action|
		this.extract(
			srcBuf, posStatsBuf, iconFtrBuf, iconStatsBuf,
			start, numFrames, action
		);
	}

}
