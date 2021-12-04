// How to use:
// point to some folder with files (addFolder)
// wait (may take time depending on size)
// move sliders

(
var fcm = FCM.new;
var histograms = [[],[],[],[]];
var loaded = false;
var stepSizes = Array.fill(4, 0);
var nSteps = 100;
var synth;

var  window = Window.new("MS",Rect(10, 10, 800, 800)).background_(Color.black)
.drawFunc_({
	4.do{|i|
		nSteps.do{|j|
			Pen.color = Color.grey(histograms[i][j]);
			Pen.addRect(Rect(i*200,j*800/nSteps,200,800 / nSteps));
			Pen.fill;
		}
	}
});

// multislider
var  multislider = MultiSliderView(window, Rect(0, 0, 800, 800))
.size_(4)
.elasticMode_(1)
.isFilled_(false)
.indexThumbSize_(200)
.valueThumbSize_(800 / nSteps)
.background_(Color.new(0,0,0,0))
.colors_(Color.clear, Color.grey)
.action_{
	if(loaded){
		fcm.index.nearest(multislider.value, {|n|
			synth.set(\bufnum, fcm.sounds[n].buffer.bufnum);

		});
	 }

};

fcm.settings.reduction.numDimensions = 4;
fcm.settings.reduction.useGrid = false;
fcm.addFolder("path/to/folder")
.makeIndex().run{
	fcm.index.indexDS.dump{|data|
		4.do{|i|
			histograms[i] = data["data"].values
			               .collect{|x|x[i]}.histo.normalize;
		};
		synth = Synth(\granular);
		loaded = true;
		defer{
			window.front;
			window.view.refresh;
		}
	}

};

SynthDef(\granular, { |bufnum|
    var rate = 10;
	var dur = 8/rate;
	var imp = Impulse.ar(rate);
	var pos = 0.5*BufDur.kr(bufnum) +(0.2*Integrator.kr(BrownNoise.kr(0.001)));
	Out.ar(0,
		TGrains.ar(1, imp, bufnum, 1, pos, dur, 0, 0.1)!2)
}).add;

)

