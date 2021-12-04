FCMDisplaySetings{
	var <>iconStyle = \wave; // circle, square, wave, fill
	var <>iconSize = 15;
	check{^true;}
}

FCMSlicingSetings{
	var <>algorithm = \onsets;
	var <>winSize = 1024;
	var <>hopSize = 512;
	var <>fftSize = 2048;
	var <>minSliceLength = 0.5;
	var <>threshold = 0.9;
	check{^true;}
}


FCMReductionSettings{
	var <>algorithm = \umap;
	var <>numNeighbors = 5;
	var <>numDimensions = 2;
	var <>useGrid = true;
	var <>gridSample = 2;
	var <>gridExtent = 0;
	var <>gridCols = 0;
	var <>gridRows = 0;
}

FCMAnalysisSettings{
	var <>positionFtr = \mfcc;
	var <>shapeFtr = \loudness;
	var <>colorFtr = \spectral_centroid;
	var <>windowSize = 1024;
	var <>hopSize = 512;
	var <>fftSize = 2048;
	var <>numDims = 13;
	var <>numDifs = 1;

	check{
		if((positionFtr == \spectralshape) && (numDims != 7))
		{"Using 7 dimensions for spectral shape".warn;^false};
		if((positionFtr == \pitch) && (numDims != 2))
		{"Using 2 dimensions for pitch".warn;^false};
		^true
	}
}


FCMSettings{
	var <>slicing,
	    <>reduction,
	    <>analysis,
	    <>display;

	*new {|
		slicing = (FCMSlicingSetings.new),
		reduction = (FCMReductionSettings.new),
		analysis = (FCMAnalysisSettings.new),
		display = (FCMDisplaySetings.new)|
		^super.newCopyArgs(slicing, reduction, analysis, display);
	}
}