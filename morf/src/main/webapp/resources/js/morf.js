function handlePollComplete(xhr, status, args) {
    if(args.stopPolling) {
    	PF('poller').stop();
    }
}

function startPoller() {
    if(!PF('poller').active) {
    	PF('poller').start();
    }
}

function start() {
   PF('statusDialog').show();
}

function stop() {
   PF('statusDialog').hide();
}