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