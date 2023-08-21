FCM{
	var <server, <>numBuffers;
	var <>settings, <pool;
	var <>slicer, <>extractor, <>projector, <index;
	var <supportedExtensions;
	var <sounds, <pending, <failed;
	var <featureDS, <mapDS, <iconStatsDS;
	var lastId;

	*new {|server = nil, nBuffers = 8192|
		^super.newCopyArgs(server, nBuffers).init;
	}

	init {|aServer|
		settings = FCMSettings.new;
		supportedExtensions = ["wav", "aiff", "aif"];
		pool = FCMPool.new(1, 0.05, 240);
		extractor = FCMFeatureExtractor.new(server, settings.analysis);
		slicer = FCMSlicer.new(server, settings.slicing);
		lastId = -1;
		sounds = IdentityDictionary.new;
		pending = List.new;
		failed = [];
		if(server.notNil.and{server.serverRunning}){
			this.prInitServerObjects;
		}
	}


	addFile {|path, segment = false|
		this.pool.addFunc({|doneAction|
			this.prAddFile(path, {
				this.prAddPending;
				doneAction.value;
			}, segment);
		}, "addFile"+path);
		^this;
	}

	addSound{|snd, segment = false|
		this.pool.addFunc({|doneAction|
			if(snd.analyzed)
			{this.prAdd(snd);}
			{this.prAddSound(snd, {
				this.prAddPending;
				doneAction.value;
			}, segment)}
		}, "addSound"+snd.path);
		^this;
	}

	addSounds {|sounds, segment = false|
		this.pool.addFunc({|doneAction|
			this.prAddSounds(sounds, doneAction, segment);
		}, "addSounds");
		^this;
	}


	addFiles {|paths, segment = false|
		this.pool.addFunc({|doneAction|
			this.prAddFiles(paths, doneAction.value,
				segment
			);
		}, "addFiles");
		^this;
	}

	addFolder {|path, segment = false|
		var supportedFiles;
		supportedFiles = PathName(path).files
		                 .select{|f|this.supported(f)}
		                 .collect{|f|f.fullPath};
		this.pool.addFunc({|doneAction|
			this.prAddFiles(supportedFiles, doneAction, segment);
		}, "addFolder");
		^this;
	}


	run {|action|
		if(server.isNil){
			server = Server.local;
			server.options.numBuffers = numBuffers;
			server.options.memSize = 1024*1024;
		};

		if(server.serverRunning.not){
			server.waitForBoot{this.prRun(action)};
		}{
			this.prRun(action);
		}
	}

	makeIndex{
		this.pool.addFunc({|doneAction|
			this.prMakeIndex(doneAction);
		});
	}

	supported {|path|
		^supportedExtensions.includesEqual(
			path.extension.toLower
		);
	}

	hasIndex{
		^index.notNil;
	}



	prRun {|action|
		if(featureDS.isNil){
			this.prInitServerObjects;
		};
		this.pool.run(action);
	}


	prInitServerObjects {
		featureDS = FluidDataSet.new(server);
		mapDS = FluidDataSet.new(server);
		featureDS = FluidDataSet.new(server);
		iconStatsDS = FluidDataSet.new(server);
		projector = FCMProjector.new(server, settings.reduction);
	}


	prAddFile {|path, action, segment = false|
		Buffer.readChannel(server, path, channels:[0], action:{|b|
			var snd = FCMSound(b, 0, b.numFrames, server, 1);
			this.prAddSound(snd, action, segment);
		});
	}

	prAddSound {|snd, action, segment = false|
		if(segment){
			this.prAddSegments(snd, action);
		}{
			snd.analyze(extractor, {
				if(snd.ftrBuffer.numFrames > 0){
					("Sound analyzed: " + snd.buffer.path).postln;
					pending.add(snd);
				}{
					("FAILED: "+" " + snd.buffer.path).postln;
					failed.add(snd);
				};
				action.value;
		   });
		}
	}

	prAddSegments{|snd, action|
		snd.slice(slicer, action:{|sounds|
			this.prAddSounds(sounds, action, false);
		});

	}


	prAddPending {|action|
		forkIfNeeded{
			while{pending.size > 0}{
				var snd = pending.pop;
				if(snd.analyzed)
				   {this.prAdd(snd)};
			};
			action.value;
		}
	}

	prAdd {|sound|
		var path = sound.buffer.path;
		var sndId = this.prNextId;
		this.sounds.add(sndId->sound);
		featureDS.addPoint( sndId.asSymbol, sound.ftrBuffer);
	    iconStatsDS.addPoint( sndId.asSymbol, sound.iconStatsBuffer;);
	}

	prAddSounds {|sounds, action, segment = false|
		var tmpPool = FCMPool.new;
		sounds.do{|snd|
			tmpPool.addFunc({|doneAction|
				this.prAddSound(snd, doneAction, segment);
			})
		};
		tmpPool.run({this.prAddPending(action)});
	}


	prAddFiles {|paths, action, segment|
		var tmpPool = FCMPool.new;
		paths.do{|path|
			path.postln;
			tmpPool.addFunc({|doneAction|
				this.prAddFile(path, doneAction, segment);
			})
		};
		tmpPool.run({this.prAddPending(action)});
	}

	prNextId { ^lastId = lastId + 1; }


	prMakeIndex {|action|
		projector.project(featureDS, mapDS);
		index = FCMIndex.new(this, action);
	}
}