function start() {
   PF('statusDialog').show();
}

function stop() {
   PF('statusDialog').hide();
}

function clearFastaInput() {
	$('#inputForm\\:inputContent').val('');
}

function pushUpdateHandler() {
   pushUpdate();
}

function handleCreateChart(xhr, status, args){
	
	//console.log(args);
	var values = JSON.parse(args.hc_values);
	console.log(values)
	var labels = JSON.parse(args.hc_labels);
	
	var seriesNames = ['MCW', 'Conservation', 'MC', 'MDC'];
	
	var tooltips = [];
	
	var tooltipCreator = function(idx, rowData) { // Purely for speed
      var s = '<b>Position: ' + idx + ", " + labels[idx] + '</b>';
      
      for (var i = 0; i < rowData.length; i++) {
         var val = vals[i];
         s += '<br/>' + seriesNames[i] + ': ' + val.toFixed(3);
      }
      return s;
	}

	var data = [[], [], [], []];
	var dataMin = 1;
	var dataMax = 0;
	var idx = 0;
   for (pos in values) {
	   var vals = values[pos];
	   tooltips[idx] = tooltipCreator(idx, vals)
	   idx+=1
	   for (var i = 0; i < vals.length; i++) {
         var val = vals[i];
         if (val > dataMax) dataMax = val;
         if (val < dataMin) dataMin = val;
         data[i].push([parseInt(pos,10), val]);
      }
	   

   }
   //console.log(data);
	
    var options = {
    	chart : {
    			renderTo: 'hc_container',
                zoomType: 'xy',
                resetZoomButton: {
                   position: {
                      // align: 'right', // by default
                      // verticalAlign: 'top', // by default
                      x: -10,
                      y: -40
                   }
                }
             },
        title: {
            text: args.hc_title,
        },
        subtitle: {
            text: 'Drag To Zoom',
            align: 'right',
            x: -100,
            y: 25,
        },
        legend : {
        	enabled: false
        },
        xAxis: {
            title: {
                text: 'Position',
                min:0
            },
        },
        yAxis: {
            title: {
                text: 'MoRF Propensity'
            },
        },
        tooltip: {
        	crosshairs:true,
            positioner: function (labelWidth, labelHeight, point) {
                return { x: Math.max(Math.min(this.chart.chartWidth - labelWidth, point.plotX-70),0), y: 50 };
            },
            headerFormat: '<b>{series.name}</b><br />',
            pointFormat: 'x = {point.x}, y = {point.y}',
            formatter:function(){
//               console.log(this)
//               var s = '<b>Position: ' + this.x + ", " + labels[this.x] + '</b>';
//
//               $.each(this.points, function () {
//                   s += '<br/>' + this.series.name + ': ' +
//                       this.y.toFixed(3);
//               });
//               return s;
               return tooltips[this.x]
               //return '<b>'+this.series.name+'</b><br/> Position: ' + this.x + ", " + labels[this.x] + "<br/> Probability: " + this.y.toFixed(3);
            },
            shared: true
        },
        plotOptions: {
           series : {
              events: {
                 legendItemClick: function(event) {

                    var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                    if (!defaultBehaviour) {

                       var seriesIndex = this.index;
                       var series = this.chart.series;

                       var reset = this.isolated;


                       for (var i = 0; i < series.length; i++)
                       {
                          if (series[i].index != seriesIndex)
                          {
                             if (reset) {
                                series[i].setVisible(true, false)
                                series[i].isolated=false;
                             } else {
                                series[i].setVisible(false, false)
                                series[i].isolated=false; 
                             }

                          } else {
                             if (reset) {
                                series[i].setVisible(true, false)
                                series[i].isolated=false;
                             } else {
                                series[i].setVisible(true, false)
                                series[i].isolated=true;
                             }
                          }
                       }
                       this.chart.redraw();

                       return false;
                    }
                 }
              }
           },
            line: {
                marker: {
                    radius: 0.5,
                    states: {
                    	hover: {
                    		enabled: false,
                    		radius: 0.5
                    	}
                    }
                },
                lineWidth: 1.5,
                states: {
                    hover: {
                        lineWidth: 1.5
                    }
                },
                threshold: null
            }
        },
        legend : {
           align : 'right',
           verticalAlign: 'top',
           layout: 'vertical',
           y:20
        },
        series: [{
        	type: 'line',
            name: 'MCW',
            data: data[0],
            color: {
               linearGradient: { x1: 0, y1: dataMin, x2: 0, y2: dataMax},
               stops: [
                   [0, Highcharts.getOptions().colors[5]],
                   [1, Highcharts.getOptions().colors[0]]
               ]
           },
           zIndex: 4
        },
        {
           type: 'line',
              name: 'Conservation',
              data: data[1],
              color: 'black',
              zIndex: 1
          },
          {
             type: 'line',
                name: 'MC',
                data: data[2],
                color: 'green',
                zIndex: 3
            },
            {
               type: 'line',
                  name: 'MDC',
                  data: data[3],
                  color: 'yellow',
                  zIndex: 2
              }]
    }
    
    var a = new Highcharts.Chart(options, function(c) {
    	setResizer(c)
    });
    
    
	
}

function setResizer(chart) {
	$(window).resize(function() {
		setTimeout(function() {
			chart.reflow();
		}, 200);
		
    });
}


$(document).ready(function() {
   console.log("spellcheck off")
   $('#inputForm\\:inputContent').attr('spellcheck',false);

   });