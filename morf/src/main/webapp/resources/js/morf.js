var MAXIMALLY_DISTINCT_COLORS = ["#990000", "#2bce48", "#005c31", "#4c005c", "#0007dc", "#808080", "#f0a3ff", "#ffcc99", "#993f00", "#94ffb5", "#8f7c00", "#9dcc00", "#c20088", "#003380", "#ffa405", "#ffa8bb", "#426600", "#ff0010", "#5ef1f2", "#00998f", "#e0ff66", "#740aff", "#191919", "#ffff80", "#ffff00", "#ff5005"];

function isUndefined( variable ) {
   return ( typeof variable === 'undefined' );
}

function handleValidateJobSubmitComplete(xhr, status, args) {
   if(args.confirm) {
      PF('noEmailDlg').show();
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

function pushUpdateHandler() {
   pushUpdate();
}

function handleCreateChart(xhr, status, args){
	
	//console.log(args);
	var values = JSON.parse(args.hc_values);
	var labels = JSON.parse(args.hc_labels);
	
	var OFFSET = 2;
	
	var seriesNames = JSON.parse(args.hc_names).slice(OFFSET);
		
//	var tooltips = [];
	
	var tooltipCreator = function(idx, rowData) { // Purely for speed
      var s = '<b>Position: ' + idx + ", " + labels[idx] + '</b>';
      
      for (var i = 0; i < rowData.length; i++) {
         var val = vals[i];
         s += '<br/>' + seriesNames[i] + ': ' + val.toFixed(3);
      }
      return s;
	}

	var data = [];
	for (var i=0;i<seriesNames.length ;i++) {
		data[i] = [];
	}
	
	
	var dataMin = 1;
	var dataMax = 0;
	var idx = 0;
	
	var morfRegions = [];
	var regionStart = null;
	var inRegion = false;
	
   for (pos in values) {
	   var vals = values[pos];
	   var posInt = parseInt(pos,10);
//	   tooltips[idx] = tooltipCreator(idx, vals)
	   idx+=1
	   var mcwVal = vals[0];
	   if ( mcwVal > 0.7 ) {
	      if (inRegion) {
	         // do nothing
	      } else {
	         // start region
	         inRegion = true;
	         regionStart = posInt;
	      }
	   } else {
        if (inRegion) {
            // end region
           inRegion = false;
           if ( ( posInt - 1) - regionStart >= 4 )  {
              morfRegions.push([regionStart, posInt - 1]);
           }
           
           regionStart = null;
         } else {
            // do nothing
         }
	   }
	   
	   
	   for (var i = 0; i < vals.length; i++) {
         var val = vals[i];
         if (val > dataMax) dataMax = val;
         if (val < dataMin) dataMin = val;
         data[i].push([posInt, val]);
      }
	   

   }
   
   if (inRegion) {
      // end region
     inRegion = false;
     morfRegions.push([regionStart, posInt]);
     regionStart = null;
   } else {
      // do nothing
   }
   
   console.log(morfRegions);
   
   
   //console.log(data);
	
    var options = {
    	chart : {
    			renderTo: 'hc_container',
                zoomType: 'x',
                resetZoomButton: {
                   position: {
                       align: 'right', // by default
                       verticalAlign: 'bottom', // by default
                      x: 0,
                      y: 26
                   }
                }
             },
        title: {
            text: args.hc_title,
        },
        subtitle: {
            text: 'Drag To Zoom',
            align: 'right',
            x: -140,
            y: 25,
        },
        legend : {
        	enabled: false
        },
        colors : MAXIMALLY_DISTINCT_COLORS,
        xAxis: {
            title: {
                text: 'Position',
                min:0
            },
            plotBands: [],
        },
        yAxis: {
            title: {
                text: 'Propensity'
            },
            startOnTick: false,
            plotLines: [{
               value: 0.7,
               color: 'red',
               dashStyle: 'shortdash',
               width: 2,
               id: 'thresholds'
//               label: {
//                   text: 'Last quarter minimum'
//               }
           }, {
               value: 0.75,
               color: 'green',
               dashStyle: 'shortdash',
               width: 2,
               id: 'thresholds'
//               label: {
//                   text: 'Last quarter maximum'
//               }
           }]
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
               var s = '<b>Position: ' + this.x + ", " + labels[this.x] + '</b>';

               $.each(this.points, function () {
                   s += '<br/>' + this.series.name + ': ' +
                       this.y.toFixed(3);
               });
               return s;
//               return tooltips[this.x]
               return '<b>'+this.series.name+'</b><br/> Position: ' + this.x + ", " + labels[this.x] + "<br/> Probability: " + this.y.toFixed(3);
            },
            shared: true
        },
        plotOptions: {
           series : {
              events: {
                 legendItemClick: function(event) {

                    var defaultBehaviour = event.browserEvent.metaKey || event.browserEvent.ctrlKey;

                    if (defaultBehaviour) {

                       var seriesIndex = this.index;
                       var series = this.chart.series;

                       var reset = this.isolated;
                       
//                       if (isUndefined(reset) ) {
//                          reset = true;
//                       }

//                       console.log(seriesIndex, series, reset, this.isolated);

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
                threshold: null
            }
        },
        legend : {
           align : 'right',
           verticalAlign: 'middle',
           layout: 'vertical',
        },
        exporting: {
           enabled: true,
           sourceWidth  : 1600,
           sourceHeight : 900,
           buttons: {
               customButton: {
                   align: 'left',
                   verticalAlign: "bottom",
                   x: 62,
                   y: 10,
                   onclick: function () {
                      if (!this.hasPlotBands) {
                         for (var i = 0; i < morfRegions.length; i++) {
                            var region = morfRegions[i];
                            a.xAxis[0].addPlotBand({
                             from: region[0],
                             to: region[1],
                             color: 'rgba(68, 170, 213, .2)',
                             id: 'MoRF-plot-bands'
                           });
                            a.yAxis[0].addPlotLine({
                               value: 0.7,
                               color: 'red',
                               dashStyle: 'shortdash',
                               width: 2,
                               id: 'thresholds'
//                               label: {
//                                   text: 'Last quarter minimum'
//                               }
                           });
                            a.yAxis[0].addPlotLine({
                               value: 0.75,
                               color: 'green',
                               dashStyle: 'shortdash',
                               width: 2,
                               id: 'thresholds'
//                               label: {
//                                   text: 'Last quarter maximum'
//                               }
                           });
   //                         options.xAxis.plotBands.push({
   //                            from: region[0],
   //                            to: region[1],
   //                            color: 'rgba(68, 170, 213, .2)'
   //                         });
                         }
//                         this.exportSVGElements[3].element.nextSibling.innerHTML = "Remove MoRF Regions";
                      } else {
                         this.xAxis[0].removePlotBand('MoRF-plot-bands');
                         this.yAxis[0].removePlotLine('thresholds');
//                         this.exportSVGElements[3].element.nextSibling.innerHTML = "Add MoRF Regions";
                      }
                      this.hasPlotBands = !this.hasPlotBands;
                   },
                   symbol: 'circle',
                   _titleKey: "myButtonTitle",
                   text: 'Toggle MoRF Bands'
               },
               yAxisSetButton: {
                  align: 'left',
                  verticalAlign: "bottom",
                  x: 225,
                  y: 10,
                  onclick: function () {
                     if (!this.yAxisBounds) {
                        this.yAxis[0].update({min:0, max:1});
                     } else {
                        this.yAxis[0].update({min:null, max:null});
                     }
                     this.yAxisBounds = !this.yAxisBounds;
                  },
                  symbol: 'circle',
                  _titleKey: "yAxisSetBoundsTitle",
                  text: 'Toggle Y-Axis Bounds'
              }
        
           }
       },
        series: []
/*        	[{
        	type: 'line',
            name: seriesNames[0],
            visible: true,
            data: data[0],
            color: {
               linearGradient: { x1: 0, y1: dataMin, x2: 0, y2: dataMax},
               stops: [
                   [0, Highcharts.getOptions().colors[5]],
                   [1, Highcharts.getOptions().colors[0]]
               ]
           },
           zIndex: data.length
        }]*/
    }
    
    // Add in plot bands
    for (var i = 0; i < morfRegions.length; i++) {
       var region = morfRegions[i];
       options.xAxis.plotBands.push({
          from: region[0],
          to: region[1],
          color: 'rgba(68, 170, 213, .2)',
          id: 'MoRF-plot-bands'
       });
    }
   
    
    // Add in additional series
    for (var i = 0; i < data.length; i++) {
		var vals = data[i];
	    options.series.push( {
	           type: 'line',
	              name: seriesNames[i],
	              visible: false,
	              isolated: false,
	              data: vals,
	              zIndex: data.length - i,
	                lineWidth: 1.5,
	                states: {
	                    hover: {
	                        lineWidth: 1.5
	                    }
	                }
	          } );
	}
    
    // Options for primary series
    options.series[0].visible=true;
    options.series[0].lineWidth=3;
    options.series[0].states.hover.lineWidth=3;
    

    
    var a = new Highcharts.Chart(options, function(c) {
    	setResizer(c);
    	c.series[0].isolated = true;
    	c.hasPlotBands = true;
    	c.yAxisBounds = false;
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
   $('body').on('click', '.custom-ui-clear-inplace', function(e) {
	   $(this).parent().siblings('.ui-inputfield').val('');
	   $(this).parent().siblings('span.ui-inplace-editor').children('button.ui-inplace-save').click();
	   
   });

   try {

      Highcharts.setOptions({
         lang: {
            myButtonTitle: "Toggle MoRF Bands",
            yAxisSetBoundsTitle: "Set Y-Axis Bounds to [0,1]"
         }
      });
   } catch(err) {

   }

   });