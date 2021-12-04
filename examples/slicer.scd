// How to use:
// drag some file for segmentation
// click on squares to play
// play with sliders and click on "update" to test segmentation parameters


(
var fcm = FCM.new;
var window = Window("", Rect(10, 10, 800, 800)).front;
var plot = FCMPlotView.new(window, window.bounds, fcm);
var sfv = SoundFileView(window).gridOn_(false);
var sl1 = Slider(w, Rect(0, 0, 20, 230)).value_(0.8);
var sl2 = Slider(w, Rect(0, 0, 20, 230)).value_(0.5);
var b1 = Button(w).states_([["Update"]]);
var b2 = Button(w).states_([["Export"]]);
var layout = HLayout(
	VLayout([plot, s:10], [sfv, s:4]),
	VLayout(StaticText(window).string_("Threshold"), sl1,
		StaticText(window).string_("Min Length"), sl2, b1, b2)
);
var slicer = FCMSlicer.new;
var sound;
var selected;



var updateColorsFunc = {
	plot.icons.do{|icon, i|
		if(icon.color.notNil && (sfv.selections[i][1] == 0)){
			sfv.setSelectionStart(i, icon.snd.start);
			sfv.setSelectionSize(i,  icon.snd.numFrames);
			sfv.setSelectionColor(i,icon.color);
		}
	};
};

var updateFunc = {
	    slicer.settings.threshold = sl1.value;
	    slicer.settings.minSliceLength = sl2.value;
		sound.slice(slicer, action:{|snds|
		snds.size.postln;
		if(snds.size > 64){
			snds.sort{|a,b| a.duration > b.duration};
			snds = snds[..63];
		};
		fcm.addSounds(snds).makeIndex().run{
			 plot.init(fcm);
			 plot.addDrawFunc(updateColorsFunc);
	      };
	  });
};

fcm.settings.reduction.gridExtent = 8;
fcm.settings.display.iconStyle = \square;
fcm.settings.display.iconSize = 30;
fcm.settings.analysis.positionFtr = \chroma;
fcm.settings.analysis.colorFtr = \pitch;

fcm.run;

sfv.selections.do({|sel, i|  sfv.selectNone(i)});

b1.action_(updateFunc);
window.layout = HLayout(VLayout(layout));
plot.canReceiveDragHandler = {
	fcm.supported(PathName(View.currentDrag));
};
plot.receiveDragHandler={|x|
	sound = FCMSound.fromFile(View.currentDrag);
	updateFunc.value;
	sfv.load(View.currentDrag);
};

window.view.acceptsMouseOver = true;

plot.mouseOverAction_{|v, x, y|
	if(fcm.hasIndex){
		var color;
		var pos = plot.unMap(x, y);
		fcm.index.nearest([pos[0], pos[1]], {|nearest|
			if(selected.notNil && (selected!=nearest)){
				plot.icons[selected].resetColor;
			};
			if(nearest!=selected){
				plot.icons[nearest].highlight;
			};
			selected = nearest;
	 });
	}
};


plot.mouseDownAction_{|v, x, y|
	if(selected.notNil){
		if(plot.icons[selected].bounds.containsPoint(x@y)){
			fcm.sounds[selected].play;
		}
	}
}
)
