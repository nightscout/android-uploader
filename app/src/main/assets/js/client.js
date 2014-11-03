var latestSGV,
    errorCode,
    treatments,
    padding = { top: 20, right: 0, bottom: 10, left: 0 },
    opacity = {current: 1, DAY: 1, NIGHT: 0.5},
    now = Date.now(),
    data = [],
    dateFn = function (d) { return new Date(d.date) },
    xScale, yScale,
    xAxis, yAxis,
    chartWidth,
    chartHeight,
    prevChartWidth = 0,
    prevChartHeight = 0,
    focusHeight,
    contextHeight,
    UPDATE_TRANS_MS = 750, // milliseconds
    brush,
    BRUSH_TIMEOUT = 300000,  // 5 minutes in ms
    brushTimer,
    brushInProgress = false,
    clip,
    TWENTY_FIVE_MINS_IN_MS = 1500000,
    THIRTY_MINS_IN_MS = 1800000,
    FORTY_MINS_IN_MS = 2400000,
    FORTY_TWO_MINS_IN_MS = 2520000,
    SIXTY_MINS_IN_MS = 3600000,
    FOCUS_DATA_RANGE_MS = 14400000,
    FORMAT_TIME = '%p', //alternate format '%H:%M'
    audio = document.getElementById('audio'),
    alarmInProgress = false,
    currentAlarmType = null,
    alarmSound = 'alarm.mp3',
    urgentAlarmSound = 'alarm2.mp3',
    WIDTH_TIME_HIDDEN = 600,
    MINUTES_SINCE_LAST_UPDATE_WARN = 10,
    MINUTES_SINCE_LAST_UPDATE_URGENT = 20,
    updateTimer,
    units = "mg/dL";

    // Tick Values
    var tickValues = [40, 60, 80, 120, 180, 300, 400];

    var futureOpacity = d3.scale.linear( )
        .domain([TWENTY_FIVE_MINS_IN_MS, SIXTY_MINS_IN_MS])
        .range([0.8, 0.1]);

    // create svg and g to contain the chart contents
    var charts = d3.select('#chartContainer').append('svg')
        .append('g')
        .attr('class', 'chartContainer')
        .attr('transform', 'translate(' + padding.left + ',' + padding.top + ')');

    var focus = charts.append('g');

    // create the x axis container
    focus.append('g')
        .attr('class', 'x axis');

    // create the y axis container
    focus.append('g')
        .attr('class', 'y axis');

    // Remove leading zeros from the time (eg. 08:40 = 8:40) & lowercase the am/pm
    function formatTime(time) {
        time = d3.time.format(FORMAT_TIME)(time);
        time = time.replace(/^0/, '').toLowerCase();
        return time;
    }

    // lixgbg: Convert mg/dL BG value to metric mmol
    function scaleBg(bg) {
        if (units == "mmol") {
            return (Math.round((bg / 18) * 10) / 10).toFixed(1);
        } else {
            return bg;
        }
    }

    // initial setup of chart when data is first made available
    function initializeCharts() {

        // define the parts of the axis that aren't dependent on width or height
        xScale = d3.time.scale()
            .domain([new Date(Date.now() - FOCUS_DATA_RANGE_MS),new Date(Date.now())]);

        yScale = d3.scale.log()
            .domain([scaleBg(30), scaleBg(510)]);

        xAxis = d3.svg.axis()
            .scale(xScale)
            .ticks(4)
            .tickFormat(d3.time.format("%H"))
            .orient('top');

        yAxis = d3.svg.axis()
            .scale(yScale)
            .tickFormat(d3.format('d'))
            .tickValues(tickValues)
            .orient('left');

        updateChart(true);
    }

    // get the desired opacity for context chart based on the brush extent
    function highlightBrushPoints(data) {
        if (data.date.getTime() >= brush.extent()[0].getTime() && data.date.getTime() <= brush.extent()[1].getTime()) {
            return futureOpacity(data.date - latestSGV.x);
        } else {
            return 0.5;
        }
    }

    function sgv2Color(sgv) {
	    if(sgv <= parseFloat(scaleBg(80))) {
		return 'red';
	    }
	    if (sgv >= parseFloat(scaleBg(180))) {
		    return 'yellow';
	    }
	    return 'green';

   }

    // called for initial update and updates for resize
    function updateChart(init) {

        console.log("Updating chart...");

        // get current data range
        var dataRange = [new Date(Date.now() - FOCUS_DATA_RANGE_MS), new Date()];

        // get the entire container height and width subtracting the padding
        chartWidth = (document.getElementById('chartContainer')
            .getBoundingClientRect().width) - padding.left - padding.right;

        chartHeight = (document.getElementById('chartContainer')
            .getBoundingClientRect().height) - padding.top - padding.bottom;

        // get the height of each chart based on its container size ratio
        focusHeight = chartHeight;

        // only redraw chart if chart size has changed
        if ((prevChartWidth != chartWidth) || (prevChartHeight != chartHeight)) {

            prevChartWidth = chartWidth;
            prevChartHeight = chartHeight;

            //set the width and height of the SVG element
            charts.attr('width', chartWidth + padding.left + padding.right)
                .attr('height', chartHeight + padding.top + padding.bottom);

            // ranges are based on the width and height available so reset
            xScale.range([0, chartWidth]);
            yScale.range([focusHeight, 0]);

            if (init) {

                // if first run then just display axis with no transition
                focus.select('.x')
                    .attr('transform', 'translate(0,' + focusHeight + ')')
                    .call(xAxis);

                focus.select('.y')
                    .attr('transform', 'translate(' + chartWidth + ',0)')
                    .call(yAxis);

                // create a clipPath for when brushing
                clip = charts.append('defs')
                    .append('clipPath')
                    .attr('id', 'clip')
                    .append('rect')
                    .attr('height', chartHeight)
                    .attr('width', chartWidth);

                // add a y-axis line that shows the high bg threshold
                focus.append('line')
                    .attr('class', 'high-line')
                    .attr('x1', xScale(new Date(Date.now() - FOCUS_DATA_RANGE_MS)))
                    .attr('y1', yScale(scaleBg(180)))
                    .attr('x2', xScale(new Date(Date.now())))
                    .attr('y2', yScale(scaleBg(180)))
                    .style('stroke-dasharray', ('3, 3'))
                    .attr('stroke', 'yellow');

                // add a y-axis line that shows the low bg threshold
                focus.append('line')
                    .attr('class', 'low-line')
                    .attr('x1', xScale(new Date(Date.now() - FOCUS_DATA_RANGE_MS)))
                    .attr('y1', yScale(scaleBg(80)))
                    .attr('x2', xScale(new Date(Date.now())))
                    .attr('y2', yScale(scaleBg(80)))
                    .style('stroke-dasharray', ('3, 3'))
                    .attr('stroke', 'red');

            } else {

                // for subsequent updates use a transition to animate the axis to the new position
                var focusTransition = focus.transition().duration(UPDATE_TRANS_MS);

                focusTransition.select('.x')
                    .attr('transform', 'translate(0,' + focusHeight + ')')
                    .call(xAxis);

                focusTransition.select('.y')
                    .attr('transform', 'translate(' + chartWidth + ', 0)')
                    .call(yAxis);

                // transition high line to correct location
                focus.select('.high-line')
                    .transition()
                    .duration(UPDATE_TRANS_MS)
                    .attr('x1', xScale(new Date(Date.now() - FOCUS_DATA_RANGE_MS)))
                    .attr('y1', yScale(scaleBg(180)))
                    .attr('x2', xScale(new Date(Date.now())))
                    .attr('y2', yScale(scaleBg(180)));

                // transition low line to correct location
                focus.select('.low-line')
                    .transition()
                    .duration(UPDATE_TRANS_MS)
                    .attr('x1', xScale(new Date(Date.now() - FOCUS_DATA_RANGE_MS)))
                    .attr('y1', yScale(scaleBg(80)))
                    .attr('x2', xScale(new Date(Date.now())))
                    .attr('y2', yScale(scaleBg(80)));
            }
        }

        // update domain
        xScale.domain(dataRange);

        // bind up the focus chart data to an array of circles
        // selects all our data into data and uses date function to get current max date
        var focusCircles = focus.selectAll('circle').data(data, dateFn);

        // if already existing then transition each circle to its new position
        focusCircles
            .transition()
            .duration(UPDATE_TRANS_MS)
            .attr('cx', function (d) { return xScale(d.date); })
            .attr('cy', function (d) { return yScale(d.sgv); })
            .attr('fill', function(d) {return sgv2Color(d.sgv);})

        // if new circle then just display
        focusCircles.enter().append('circle')
            .attr('cx', function (d) { return xScale(d.date); })
            .attr('cy', function (d) { return yScale(d.sgv); })
            .attr('fill', function(d) {return sgv2Color(d.sgv);})
            //.attr('opacity', function (d) { return futureOpacity(d.date - latestSGV.x); })
            .attr('r', function(d) { if (d.type == 'mbg') return 6; else return 2;});

        focusCircles.exit()
            .remove();

        // remove all insulin/carb treatment bubbles so that they can be redrawn to correct location
        d3.selectAll('.path').remove();

        // update x axis
        focus.select('.x.axis')
            .call(xAxis);

        // add clipping path so that data stays within axis
        focusCircles.attr('clip-path', 'url(#clip)');
    }

    // look for resize but use timer to only call the update script when a resize stops
    var resizeTimer;
    window.onresize = function () {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(function () {
            updateChart(false);
        }, 100);
    };

    function timeAgo(offset) {
        var parts = {},
            MINUTE = 60,
            HOUR = 3600,
            DAY = 86400,
            WEEK = 604800;

        //offset = (MINUTE * MINUTES_SINCE_LAST_UPDATE_WARN) + 60
        //offset = (MINUTE * MINUTES_SINCE_LAST_UPDATE_URGENT) + 60

        if (offset <= MINUTE)              parts = { label: 'now' };
        if (offset <= MINUTE * 2)          parts = { label: '1 min ago' };
        else if (offset < (MINUTE * 60))   parts = { value: Math.round(Math.abs(offset / MINUTE)), label: 'mins' };
        else if (offset < (HOUR * 2))      parts = { label: '1 hr ago' };
        else if (offset < (HOUR * 24))     parts = { value: Math.round(Math.abs(offset / HOUR)), label: 'hrs' };
        else if (offset < DAY)             parts = { label: '1 day ago' };
        else if (offset < (DAY * 7))       parts = { value: Math.round(Math.abs(offset / DAY)), label: 'day' };
        else if (offset < (WEEK * 52))     parts = { value: Math.round(Math.abs(offset / WEEK)), label: 'week' };
        else                               parts = { label: 'a long time ago' };

        if (offset > (MINUTE * MINUTES_SINCE_LAST_UPDATE_URGENT)) {
            var lastEntry = $("#lastEntry");
            lastEntry.removeClass("warn");
            lastEntry.addClass("urgent");

            $(".bgStatus").removeClass("current");
        } else if (offset > (MINUTE * MINUTES_SINCE_LAST_UPDATE_WARN)) {
            var lastEntry = $("#lastEntry");
            lastEntry.removeClass("urgent");
            lastEntry.addClass("warn");
        } else {
            $(".bgStatus").addClass("current");
            $("#lastEntry").removeClass("warn urgent");
        }

        if (parts.value)
            return parts.value + ' ' + parts.label + ' ago';
        else
            return parts.label;

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //draw a compact visualization of a treatment (carbs, insulin)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    function drawTreatment(treatment, scale, showValues) {
        var carbs = treatment.carbs;
        var insulin = treatment.insulin;
        var CR = treatment.CR;

        var R1 = Math.sqrt(Math.min(carbs, insulin * CR)) / scale,
            R2 = Math.sqrt(Math.max(carbs, insulin * CR)) / scale,
            R3 = R2 + 8 / scale;

        var arc_data = [
            { 'element': '', 'color': '#9c4333', 'start': -1.5708, 'end': 1.5708, 'inner': 0, 'outer': R1 },
            { 'element': '', 'color': '#d4897b', 'start': -1.5708, 'end': 1.5708, 'inner': R1, 'outer': R2 },
            { 'element': '', 'color': 'transparent', 'start': -1.5708, 'end': 1.5708, 'inner': R2, 'outer': R3 },
            { 'element': '', 'color': '#3d53b7', 'start': 1.5708, 'end': 4.7124, 'inner': 0, 'outer': R1 },
            { 'element': '', 'color': '#5d72c9', 'start': 1.5708, 'end': 4.7124, 'inner': R1, 'outer': R2 },
            { 'element': '', 'color': 'transparent', 'start': 1.5708, 'end': 4.7124, 'inner': R2, 'outer': R3 }
        ];

        if (carbs < insulin * CR) arc_data[1].color = 'transparent';
        if (carbs > insulin * CR) arc_data[4].color = 'transparent';
        if (carbs > 0) arc_data[2].element = Math.round(carbs) + ' g';
        if (insulin > 0) arc_data[5].element = Math.round(insulin * 10) / 10 + ' U';

        var arc = d3.svg.arc()
            .innerRadius(function (d) { return 5 * d.inner; })
            .outerRadius(function (d) { return 5 * d.outer; })
            .endAngle(function (d) { return d.start; })
            .startAngle(function (d) { return d.end; });

        var treatmentDots = focus.selectAll('treatment-dot')
            .data(arc_data)
            .enter()
            .append('g')
            .attr('transform', 'translate(' + xScale(treatment.x) + ', ' + yScale(scaleBg(treatment.y)) + ')');

        var arcs = treatmentDots.append('path')
            .attr('class', 'path')
            .attr('fill', function (d, i) { return d.color; })
            .attr('id', function (d, i) { return 's' + i; })
            .attr('d', arc);


        // labels for carbs and insulin
        if (showValues) {
            var label = treatmentDots.append('g')
                .attr('class', 'path')
                .attr('id', 'label')
                .style('fill', 'white');
            label.append('text')
                .style('font-size', 30 / scale)
                .style('font-family', 'Arial')
                .attr('text-anchor', 'middle')
                .attr('dy', '.35em')
                .attr('transform', function (d) {
                    d.outerRadius = d.outerRadius * 2.1;
                    d.innerRadius = d.outerRadius * 2.1;
                    return 'translate(' + arc.centroid(d) + ')';
                })
                .text(function (d) { return d.element; })
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // function to predict
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    function predictAR(actual) {
        var ONE_MINUTE = 60 * 1000;
        var FIVE_MINUTES = 5 * ONE_MINUTE;
        var predicted = [];
        var BG_REF = scaleBg(140);
        var BG_MIN = scaleBg(36);
        var BG_MAX = scaleBg(400);
        // these are the one sigma limits for the first 13 prediction interval uncertainties (65 minutes)
        var CONE = [0.020, 0.041, 0.061, 0.081, 0.099, 0.116, 0.132, 0.146, 0.159, 0.171, 0.182, 0.192, 0.201];
        if (actual.length < 2) {
            var y = [Math.log(actual[0].sgv / BG_REF), Math.log(actual[0].sgv / BG_REF)];
        } else {
            var elapsedMins = (actual[1].date - actual[0].date) / ONE_MINUTE;
            if (elapsedMins < 5.1) {
                y = [Math.log(actual[0].sgv / BG_REF), Math.log(actual[1].sgv / BG_REF)];
            } else {
                y = [Math.log(actual[0].sgv / BG_REF), Math.log(actual[0].sgv / BG_REF)];
            }
        }
        var AR = [-0.723, 1.716];
        var dt = actual[1].date.getTime();
        var predictedColor = 'blue';
//        if (browserSettings.theme == "colors") {
//            predictedColor = 'cyan';
//        }
        for (var i = 0; i < CONE.length; i++) {
            y = [y[1], AR[0] * y[0] + AR[1] * y[1]];
            dt = dt + FIVE_MINUTES;
            // Add 2000 ms so not same point as SG
            predicted[i * 2] = {
                date: new Date(dt + 2000),
                sgv: Math.max(BG_MIN, Math.min(BG_MAX, Math.round(BG_REF * Math.exp((y[1] - 2 * CONE[i]))))),
                color: predictedColor
            };
            // Add 4000 ms so not same point as SG
            predicted[i * 2 + 1] = {
                date: new Date(dt + 4000),
                sgv: Math.max(BG_MIN, Math.min(BG_MAX, Math.round(BG_REF * Math.exp((y[1] + 2 * CONE[i]))))),
                color: predictedColor
            };
            predicted.forEach(function (d) {
                d.type = 'forecast';
                if (d.sgv < BG_MIN)
                    d.color = "transparent";
            })
        }
        return predicted;
    }

    function updateChartWithTimer() {
        console.log("Timer expired...updating chart...");
        updateChart(false);
        updateTimer = setTimeout(updateChartWithTimer, 60000);
    }

    function updateData(newData) {
        clearTimeout(updateTimer);
        if (newData != null) {
            data = newData.map(function (obj) {
                return { date: new Date(obj.date), sgv: scaleBg(obj.sgv), type: 'sgv'}
            });
        }
        isInitialData = false;
        updateChart(false);
        updateTimer = setTimeout(updateChartWithTimer, 60000);
    }

    function updateUnits(isMmol) {
        if (isMmol)  {
            console.log("changing to mmol");
            tickValues = [2.0, 3.0, 4.0, 6.0, 10.0, 15.0, 22.0];
            units = "mmol";
        } else {
            console.log("changing to mg/dl");
            tickValues = [40, 60, 80, 120, 180, 300, 400];
            units = "mg/dL";
        }

        yScale = d3.scale.log()
            .domain([scaleBg(30), scaleBg(510)]);

        yAxis = d3.svg.axis()
            .scale(yScale)
            .tickFormat(d3.format('d'))
            .tickValues(tickValues)
            .orient('left');

        focus.select('.y.axis').remove();

        yScale.range([focusHeight, 0]);

        focus.append('g')
            .attr('class', 'y axis');

        focus.select('.y')
            .attr('transform', 'translate(' + chartWidth + ', 0)')
            .call(yAxis);

        data = data.map(function (obj) {
            return { date: new Date(obj.date), sgv: scaleBg(obj.sgv), type: 'sgv'}
        });
        
        isInitialData = false;
        updateChart(false);
    }

    // Initialize Charts
    var isInitialData = true;
    initializeCharts();
    updateTimer = setTimeout(updateChartWithTimer, 60000);