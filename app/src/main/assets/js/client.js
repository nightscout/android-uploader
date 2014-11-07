var padding = { top: 20, right: 0, bottom: 10, left: 0 },
    data = [],
    dateFn = function (d) { return new Date(d.date) },
    xScale, yScale,
    xAxis, yAxis,
    chartWidth,
    chartHeight,
    prevChartWidth = 0,
    prevChartHeight = 0,
    focusHeight,
    UPDATE_TRANS_MS = 750,
    brush,
    clip,
    FOCUS_DATA_RANGE_MS = 14400000,
    updateTimer,
    units = "mg/dL";

    // Tick Values
    var tickValues = [40, 60, 80, 120, 180, 300, 400];

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

    // Convert mg/dL BG value to metric mmol
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
                    .attr('stroke', 'grey');

                // add a y-axis line that shows the low bg threshold
                focus.append('line')
                    .attr('class', 'low-line')
                    .attr('x1', xScale(new Date(Date.now() - FOCUS_DATA_RANGE_MS)))
                    .attr('y1', yScale(scaleBg(80)))
                    .attr('x2', xScale(new Date(Date.now())))
                    .attr('y2', yScale(scaleBg(80)))
                    .style('stroke-dasharray', ('3, 3'))
                    .attr('stroke', 'grey');

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
            .attr('fill', function (d) { return d.color; });

        // if new circle then just display
        focusCircles.enter().append('circle')
            .attr('cx', function (d) { return xScale(d.date); })
            .attr('cy', function (d) { return yScale(d.sgv); })
            .attr('fill', function (d) { return d.color; })
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
        // only update if units have changed
        if (isMmol && units != "mmol")  {
            console.log("changing to mmol");
            tickValues = [2.0, 3.0, 4.0, 6.0, 10.0, 15.0, 22.0];
            units = "mmol";
            data = data.map(function (obj) {
                return { date: new Date(obj.date), sgv: (Math.round((obj.sgv / 18) * 10) / 10).toFixed(1), type: 'sgv'}
            });
        } else if (!isMmol && units != "mg/dL") {
            console.log("changing to mg/dl");
            tickValues = [40, 60, 80, 120, 180, 300, 400];
            units = "mg/dL";
            data = data.map(function (obj) {
                return { date: new Date(obj.date), sgv: obj.sgv * 18, type: 'sgv'}
            });
        }


        // remove the data that was staled when timers were paused
        focus.selectAll('circle').data([], dateFn).exit().remove();

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

        isInitialData = false;
        updateChart(false);
    }

    // Initialize Charts
    var isInitialData = true;
    initializeCharts();
    updateTimer = setTimeout(updateChartWithTimer, 60000);