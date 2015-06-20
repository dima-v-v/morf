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

function clearFastaInput() {
	$('#inputForm\\:inputContent').val('');
}

function handleCreateChart(xhr, status, args){
	
	console.log(args);
	var values = JSON.parse(args.hc_values);
	var labels = JSON.parse(args.hc_labels);

	var data = [];
	var dataMin = 1;
	var dataMax = 0;
   for (pos in values) {
	   var val = values[pos];
	   data.push([parseInt(pos,10), val]);
	   if (val > dataMax) dataMax = val;
	   if (val < dataMin) dataMin = val;
   }
   console.log(data);
	
    var options = {
    	chart : {
    			renderTo: 'hc_container',
                zoomType: 'xy',
                resetZoomButton: {
                   position: {
                      // align: 'right', // by default
                      // verticalAlign: 'top', // by default
                      x: -10,
                      y: -30
                   }
                }
             },
        title: {
            text: 'MoRF Potential vs Position',
        },
        legend : {
        	enabled: false
        },
        xAxis: {
            title: {
                text: 'Position',
                min:0,
                max:2500
            },
        },
        yAxis: {
            title: {
                text: 'MoRF Probability'
            },
        },
        tooltip: {
            positioner: function (labelWidth, labelHeight, point) {
                return { x: Math.max(Math.min(this.chart.chartWidth - labelWidth, point.plotX-70),0), y: 50 };
            },
            headerFormat: '<b>{series.name}</b><br />',
            pointFormat: 'x = {point.x}, y = {point.y}',
            formatter:function(){
               return '<b>'+this.series.name+'</b><br/> Position: ' + this.x + ", " + labels[this.x] + "<br/> Probability: " + this.y;
            }
        },
        plotOptions: {
            line: {
                color: {
                    linearGradient: { x1: 0, y1: dataMin, x2: 0, y2: dataMax},
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.getOptions().colors[5]]
                    ]
                },
                marker: {
                    radius: 2
                },
                lineWidth: 1,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                threshold: null
            },
            area: {
                fillColor: {
                    linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
                    stops: [
                        [0, Highcharts.getOptions().colors[0]],
                        [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                    ]
                },
                marker: {
                    radius: 2
                },
                lineWidth: 1,
                states: {
                    hover: {
                        lineWidth: 1
                    }
                },
                threshold: null
            }
        },
        series: [{
        	type: 'line',
            name: 'MoRF Potential',
            data: data
        }]
    }
    
    a = new Highcharts.Chart(options);
	
}

$(document).ready(function() {
   console.log("spellcheck off")
   $('#inputForm\\:inputContent').attr('spellcheck',false);

   });