FCMSlicer {
	var server, <>settings;

	*new{|server = (Server.default),
		  settings = (FCMSlicingSetings.new)|
		^super.newCopyArgs(server, settings);
	}

	run{|srcBuf, start, numFrames, action|
		settings.check;
		^this.slice(
			srcBuf,start, numFrames, action
		);
	}

	prFixSlices{|slices, end|
		if(slices.first != 0){
			slices = [0] ++ slices
		};
		if(slices.last != end){
			slices = slices ++ [end]
		};
		^slices;
	}

	slice{|sound, action|
		var indices = Buffer.new;
		var slicer = settings.algorithm.switch
		{\onsets}{FluidBufOnsetSlice}
		{\novelty}{FluidBufNoveltySlice}
		{\transients}{FluidBufTransientSlice};
		slicer.process(
			server, sound.buffer, sound.start,
			sound.numFrames, 0, -1, indices,
			threshold: settings.threshold,
			minSliceLength:settings.minSliceLength,
			action:{
				indices.loadToFloatArray(action:{|arr|
					var sounds = [];
					if(arr.size > 1){
						var slices = this.prFixSlices(arr, sound.numFrames);
						slices.doAdjacentPairs({|a,b|
							sounds = sounds.add(
								FCMSound(sound.buffer, a, b, server)
							);
						})
					}
					{
							sounds = [sound];
					};
					action.value(sounds);
				})
			}
		)
	}
}