FCMSound {
	var <server, <buffer;
	var <start, <end;
	var <numChannels;
	var <ftrBuffer;
	var <iconBuffer;
	var <iconStatsBuffer;
	var iconStats;
	var <slices;

	*new{|buffer, start = 0, end = -1,
		  server = (Server.default),
		  numChannels = 1|
		if(end == -1) {end = buffer.numFrames};
	  ^super.newCopyArgs(
			server, buffer, start, end, numChannels
		);
	}


	*fromFile{|path, start = 0, end = -1,
		       server = (Server.default), numChannels = 1|
		var sf  = SoundFile.openRead(path);
		var buf;
		sf.sampleRate.postln;
		if(end == -1) {end = sf.numFrames};
		buf = Buffer.readChannel(
			server, path, start,
			end - start,
			(0..(numChannels -1)),
		);
		^super.newCopyArgs(
			server, buf, start, end, numChannels
		);
	}

	analyze{|extractor = (FCMFeatureExtractor.new),
		     action = ({"analysis done".postln})|
		forkIfNeeded{
			ftrBuffer = Buffer.new(server);
			iconBuffer = Buffer.new(server);
			iconStatsBuffer = Buffer.new(server);
			extractor.run(buffer, ftrBuffer,iconBuffer, iconStatsBuffer,
				start, this.numFrames, action);
		}
	}

	analyzeIcon{|extractor = (FCMIconFeatureExtractor.new),
		     action = ({"icon analysis done".postln})|
		iconBuffer = Buffer.new(server);
		extractor.run(buffer, iconBuffer,
			start, this.numFrames, action);
	}

	slice{|slicer = (FCMSlicer.new)
		 action = ({"done slicing".postln})|
		slicer.run(this, {|s| slices = s; action.value(s)})
	}



	numFrames{^end - start}

	duration{
		^buffer.isNil.if{0}
		{this.numFrames / buffer.sampleRate}
	}

	path{
		^buffer.isNil.if{nil}
		{buffer.path}
	}

	loaded{
		^buffer.isNil.if{false}
		{(buffer.numFrames > 0)}
	}


	analyzed{
		^ftrBuffer.isNil.if{false}
		{(ftrBuffer.numFrames > 0)}
	}

	play{
		{
			PlayBuf.ar(1, buffer, BufRateScale.kr(buffer), startPos:start)*
			EnvGen.ar(Env.linen(0, this.duration, 0, 1), doneAction:2);
		}.play
	}
}

