FCMProjector{
	var server, <settings;
	var algorithm, grid;
	var tmpDS;
	*new{|server = (Server.default),
		  settings = (FCMReductionSettings.new)|
		^super.newCopyArgs(server, settings).init;
	}


	init{
		algorithm = settings.algorithm.switch
		{\pca}{FluidPCA.new(server)}
		{\mds}{FluidMDS.new(server)}
		{\umap}{FluidUMAP.new(server, minDist:0.7)};
		algorithm.numDimensions_(settings.numDimensions);
		if(settings.algorithm==\umap){
			algorithm.numNeighbours_(settings.numNeighbors);
		};
		tmpDS = FluidDataSet.new(server);
		grid = FluidGrid(server, settings.gridSample, settings.gridExtent);
	}

	project{|srcDS, tgtDS, action|
		if(settings.useGrid && (settings.numDimensions == 2)){
			forkIfNeeded{
				algorithm.fitTransform(
					srcDS, tmpDS
				);
				server.sync;
				grid.fitTransform(tmpDS, tgtDS, action:action);
			}
		}{
			algorithm.fitTransform(
				srcDS, tgtDS, action:action
			);
		}
	}
}
