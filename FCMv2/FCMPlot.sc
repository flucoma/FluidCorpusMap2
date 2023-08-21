FCMPlotView : UserView{
	var plotSpecs, sizeSpec;
	var <>settings;
	var <>minPointSize = 3, <>maxPointSize = 30;
	var <>pointColor;
	var <>map;
	var <>icons;
	var <>margin = 50;
	var drawFuncs;
	var <>colorFunc;
	var <>size;


	*new{|parent, bounds, aMap|
		^super.new(parent, bounds).init(aMap);
	}

	init {|aMap|
		this.map = aMap;
		this.settings = this.map.settings.display;
		this.background_(Color.black);
		this.resize_(5);
		this.animate = true;
		this.frameRate = 10;
		this.size = settings.iconSize;
		this.colorFunc = {|v| Color.hsv(v * 0.9, 0.7, 0.7)};
		//this.colorFunc = {|v| Color.grey(0.3+(0.7*v))};
		drawFuncs = [];
		this.drawFunc = {this.draw; drawFuncs.do{|f|f.value(this)}};
		this.pointColor = Color.new(0,0,1,0.7);
		this.icons = aMap.sounds.collect{|snd| FCMSoundPlot.new(snd, this)};
		this.refresh;
	}

	addDrawFunc{|f|
		drawFuncs.add(f);
	}

	draw{
		if(map.index.notNil){
			this.makeSpecs(size/2);
			map.index.positions.do{|pos, i|
				var x = plotSpecs[0].map(pos[0]).asInteger - (size / 2);
				var y = plotSpecs[1].map(pos[1]).asInteger - (size  / 2);
				if(this.icons[i].notNil){
					this.icons[i].draw(Rect(x,y, size, size), settings.iconStyle)
				};
			};
		}
	}


	makeSpecs {|size|
		plotSpecs = [
			ControlSpec(size, this.bounds.width - size),
			ControlSpec(size, this.bounds.height - size)
		];
		//sizeSpec = ControlSpec(minPointSize, maxPointSize);
	}
	unMap{|x,y|
		^[plotSpecs[0].unmap(x), plotSpecs[1].unmap(y)]
	}
}


FCMSoundPlot{
	var array;
	var iconStats;
	var shapeVal;
	var colorVal;
	var resamp;
	var <snd;
	var parent, index;
	var <>color;
	var <>originalColor;
	var minSamples, maxSamples, meanSamples;
	var <>x, <>y;
	var <>bounds;


	*new{|sound, parent_|
		^ super.new.init(sound, parent_);
	}

	resetColor{
		color = originalColor;
	}

	highlight{|amount = 0.5|
		color = originalColor.blend(Color.white, amount);

	}

	resamplingNeeded{|size|
	 ^(minSamples.isNil || (minSamples.size != size))
	}

	resample{|size|
			var step = array.size / size;
			var max = Array.fill(size,0);
			var min = Array.fill(size,0);
		    var mean = Array.fill(size,0);
			size.do{|i|
				var start = (i*step).floor.min(array.size - 1).asInteger;
				var end = ((i+1)*step).floor.min(array.size - 1).asInteger;
				max[i] = array[start..end].maxItem;
				min[i] = array[start..end].minItem;
			    mean[i] = array[start..end].mean;
			};

		maxSamples = this.normalize(max);
		minSamples = this.normalize(min);
		meanSamples = this.normalize(mean);
	}

	normalize{|x|
		^x.collect{|v|
			((v - index.iconMinima[0]) /
				(index.iconMaxima[0] - index.iconMinima[0])
			).clip(0,1);
		}
	}


	init{|sound, parent_|
		snd = sound;
		parent = parent_;
		index  = parent.map.index;
		array = nil;
		{
		sound.iconBuffer.loadToFloatArray(action:{|arr, buf|
			array = arr.unlace(2)[0];
		});
		sound.iconStatsBuffer.loadToFloatArray(action:{|arr, buf|
			iconStats = arr;
			shapeVal = (arr[0] - index.iconMinima[0]) / (index.iconMaxima[0] - index.iconMinima[0]);
			colorVal = (arr[7] - index.iconMinima[1]) / (index.iconMaxima[1] - index.iconMinima[1]);
			color = parent.colorFunc.value(colorVal);
			originalColor = color;
		});}.fork(AppClock)

	}

	updateResamp{|size|
		if (resamp.isNil || (resamp.size != size)){
			resamp = array.resamp1(size).normalize();
		};
	}

	draw{|bounds, type|
		this.x = bounds.left;
		this.y = bounds.top;
		this.bounds = bounds;
		switch(type,
			\circle, {this.drawShape(\circle, bounds)},
			\square, {this.drawShape(\square, bounds)},
			\wave, {this.drawWave(bounds)},
			\fill, {this.drawFill(bounds)}
		);
	}

	drawWave{|bounds|
		if(array.notNil){
			var h = bounds.height;
			if(this.resamplingNeeded(bounds.width)){this.resample(bounds.width)};
		    Pen.strokeColor = color;
			Pen.addRect(bounds);
			bounds.width.do{|i|
				Pen.line((bounds.left + i)@(bounds.top + h- (h*minSamples[i])),
					(bounds.left + i)@(bounds.top + h - (h*maxSamples[i])));

			};
			Pen.stroke;
		}
	}

	drawFill{|bounds|
		if(array.notNil){
			var h = bounds.height;
			if(this.resamplingNeeded(bounds.width)){this.resample(bounds.width)};
		    Pen.strokeColor = color;
			Pen.addRect(bounds);
			bounds.width.do{|i|
				Pen.line((bounds.left + i)@(bounds.top + h),
					(bounds.left + i)@(bounds.top + h - (h*meanSamples[i])));

			};
			Pen.stroke;
		}

	}

	drawShape {|shape, bounds|
		var size = bounds.width;
		var off = size / 2;
		Pen.strokeColor = color;
		Pen.fillColor = color;
		switch(shape,
			\square, {Pen.addRect(bounds)},
			\circle, {Pen.addOval(bounds)}
		);
		Pen.fill;
	}

}
