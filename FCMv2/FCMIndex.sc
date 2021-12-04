FCMIndex{
	var map;
	var <tree, <posNormalizer, <iconNormalizer;
	var <posMaxima, <posMinima;
	var <iconMaxima, <iconMinima;
	var <indexDS;
	var <positions;

	*new{|aMap, doneAction, normalize = true|
		^super.newCopyArgs(aMap).init(doneAction, normalize);
	}

	init{|doneAction, normalize|
		forkIfNeeded{
			tree = FluidKDTree.new(map.server);
			posNormalizer = FluidNormalize.new(map.server);
			iconNormalizer = FluidRobustScale.new(map.server);

			map.server.sync;
			posNormalizer.fit(map.mapDS);
			iconNormalizer.fit(map.iconStatsDS);
			normalize.if{
				indexDS = FluidDataSet.new(map.server);
				posNormalizer.transform(map.mapDS, indexDS);
				map.server.sync;
			}{
				indexDS = map.mapDS;
			};

			positions = Array.fill(map.sounds.size, [0,0]);
			tree.fit(indexDS);
			map.server.sync;
			indexDS.dump{|dict|
				dict["data"].keysDo{|x|
					positions[x.asInteger] = dict["data"][x];
				};
			};

			posNormalizer.dump{|dict|
				posMaxima = dict["data_max"];
				posMinima = dict["data_min"];
			};

			iconNormalizer.dump{|dict|
				iconMaxima = [dict["data_high"][6]]++[dict["data_high"][13]];
				iconMinima = [dict["data_low"][4]]++[dict["data_low"][11]];
			};

			map.server.sync;
			doneAction.value;
		};
	}

	nearest{|point, action|
		var tmpbuf = Buffer.loadCollection(
			map.server, point, 1, {|b|
				tree.kNearest(b,{|result|
					action.value(result.asInteger);
				});
		});
	}
}