// A quick guide for using the library.


// FCMSound
~sound = FCMSound.new(buf, from, to)
~sound = FCMSound.fromFile("path", from, to)
~sound.analyze(extractor, action)// extractor is a FCMFeatureExtractor
// when action runs, ~sound has features in ftrBuffer

~sound.slice(slicer, action)// extractor is a FCMSlicer
// action is passed a collection of FCMSounds


// FCM
// Actions on FCM are only executed when you call _run_
~fcm = FCM.new(server:Server.local, nBuffers:8192); // nil server creates a new one, with defaul 8192 and lots of memory

~fcm.addFile("path_to_file") // load , analyze, add to dataset
~fcm.addFiles(["path", "path"...])
~fcm.addFolder(["path_to_folder"])

~fcm.addSounds([sound, sound])// add a collection of FCMSound

~fcm.makeIndex //runs dimensionality reduction, normalization of the reduced space, and fit a KDTree on it for queries

~fcm.run // now run everything

// Using method chaining:
FCM.new().addFolder("xxx").makeIndex().run()



// FCMPlotView
~fcmPlot = FCMPlotView.new(window, bounds, ~fcm); /
~fcmPlot.addDrawFunc(f) // add additional function when draw is called, for e.g. animating, highlighting


~fcm.settings // an instance of FCMSettings, which contains all the settings structure

// Example:
~fcm.settings.analysis.positionFtr = \chroma; // use chroma as the feature that controls the positions in the reduced space



