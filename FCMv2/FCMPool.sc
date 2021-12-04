FCMPool{
	var numParallel, delta, timeOut, <>doneAction;
	var <>cancelled;
	var <running, <queued;
	var timeOut;

	*new{|num = 200, delta = 0.1, timeOut = 50|
		^super.newCopyArgs(num, delta, timeOut).init;
    }


    init{
		queued = List.new;
		running = List.new;
		cancelled = false;
	}

	add{|job|
		queued.addFirst(job);
	}

	addJob{|job|
		queued.addFirst(job);
	}

	addFunc{|func, str = ""|
		this.addJob(
			FCMJob(func, str);
		)
	}

	cancel{
		cancelled = true;
	}

	run{|action|
		this.doneAction = action;
		cancelled = false;
		AppClock.sched(delta, {this.update});
	}

	print{
		running.do{|j|
			j.id.post;
			" ".post;
		};
		"".postln;
	}

	update{
		var currentTime = Process.elapsedTime;
		if(cancelled || (this.queued.isEmpty && running.isEmpty)){
			doneAction.value;
			^nil;
		};

		running.do{|j|
			if((currentTime - j.startTime) > timeOut){
				postln("timeout "+j.id+" "+j.str);
				running.remove(j);
				postln(running.size+" "+numParallel+" "+queued.size );
			};
		};
		if( (running.size < numParallel) &&
			(queued.size > 0)
		){
			(numParallel - running.size).do{
				var job = queued.pop;
				if(job.notNil){
					running.add(job);
					job.run(action:{|j|
						running.remove(j);
					});
				}
			}
		};
		^delta;
	}
}



FCMJob{
	classvar nextId;
	var <id, <str, func;
	var <startTime;
	var <doneFunc;

	*initClass{
		nextId = 0;
	}

	*new{|f, string=""|
		nextId = nextId + 1;
		^super.new.init(nextId, f, string);
    }

	init{|jobId, f, string|
		id = jobId;
		func = f;
		str = string;
	}

	run{|action|
		startTime = Process.elapsedTime;
		func.value({action.value(this)});
	}
}