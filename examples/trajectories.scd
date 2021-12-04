// How to use:
// point to some folder with files (addFolder)
// play with mouse
// record a trajectory:
// press shift and a number key (top row)
// click and drag mouse
// when a trajectory is recorded, play it with same number key


(
var win, plot, layout, playerFunc, preListen;
var recording = -1, lastTime = 0;
var data = Array.fill(8, List.new);
var players = Array.fill(8, nil);
var synths = Array.fill(8, nil);
var fcm = FCM.new()
.addFolder("path/to/folder/")
.makeIndex()
.run{
	defer{
		   var recordingColor = {recording.switch(-1, Color.black, Color.grey)};
		   var makePlayer = {|i|
			  Routine{
					var pos = 0;
				     synths[i] = playerFunc.play;
				     s.sync;
				     synths[i].set(\bufnum, data[i][pos][2]);
				     synths[i].set(\gate, 1);
				     while({pos < data[i].size}, {
						data[i][pos][3].wait;
						synths[i].set(\bufnum, data[i][pos][2]);
						pos = pos + 1;
				    });
				    synths[i].free;
				};
	       };

		   playerFunc =  { |bufnum = 0, gate = 0|
			   EnvGen.ar(Env.adsr, gate) * PlayBuf.ar(1, bufnum, 1, gate, loop:1);
           };
		   preListen = playerFunc.play;
		   win = Window.new("", Rect(0, 0, 800, 800));
		   win.view.acceptsMouseOver_(true);
		   plot = FCMPlotView.new(win, win.bounds, fcm);
		   plot.addDrawFunc{
			data.do{|d, i|
				if(d.size > 0){
					if(players[i].isPlaying){
						Pen.strokeColor = Color.white;
					}{
						Pen.strokeColor = Color.grey(0.2+(0.07*(i+1)));
					};

					Pen.moveTo(d[0][0]@d[0][1]);
					d.do{|r|
						Pen.lineTo(r[0]@r[1]);
					};
					Pen.stroke;
				}
			}
		   };
		   win.layout = VLayout([plot, s:10]);
		   layout = HLayout.new;
		   8.do{|i| layout.add(
			     UserView.new(win, Rect(0,0,20,20))
			       .background_(Color.grey(0.2+(0.07*(i+1)))))
		   };
		   win.layout.add(layout, stretch:1);

		   // KEYDOWN
		   win.view.keyDownAction_{|v, c, m, u, k|
			 if((k.asInteger >= 18) && (k.asInteger <= 26)){
				var index =  k.asInteger - 17;

				if(m.isShift){
				     recording = index;
				     win.view.background_(recordingColor.value);
				}{
					if((data[index - 1].size > 0) &&
						players[index - 1].isPlaying.not){
						players[index - 1] = makePlayer.value(index - 1);
						players[index - 1].play(SystemClock);
					}
				  };
			    };
		    };

		    // KEYUP
		    win.view.keyUpAction_{|v, c,   m, u, k|
			  var index =  k.asInteger - 17;
			if((k.asInteger >= 18) &&
				(k.asInteger <= 26) &&
				(recording > 0)){
				recording = -1;
			};

			if((k.asInteger >= 18)
				&& (k.asInteger <= 26)
				&& players[index - 1].isPlaying){
				   synths[index - 1].free;
			};
			win.view.background_(recordingColor.value);

		    };

		    // MOUSEDOWN
		    plot.mouseDownAction_{
			  preListen.set(\gate, 1);
			  if(recording > 0){
				data[recording - 1] = List.new;
				lastTime = thisThread.seconds;
			  };
		    };

		    //MOUSEUP
		    plot.mouseUpAction_{
			    preListen.set(\gate, 0);
			    recording = -1;
			    win.view.background_(recordingColor.value);
		    };

		    // MOUSEMOVE
		    plot.mouseMoveAction_{|v, x, y|
			    var pos = plot.unMap(x, y);
				fcm.index.nearest([pos[0], pos[1]], {|nearest|
				    var buf = fcm.sounds[nearest].buffer.bufnum;
					preListen.set(\bufnum, buf);
				    if(recording > 0){
					   var record = [x, y, buf, thisThread.seconds - lastTime];
					    defer{lastTime = thisThread.seconds};
				        data[recording - 1].add(record);
			        };
				 });
		    };
		    win.view.background_(recordingColor.value);
			win.front;
		}
	};
)
