
function ChartModel(params,element) {
  var self = this;

  self.width = params.width ? params.width : 450;
  self.height = params.height ? params.height : 200;

  self.observed = params.observed;
  self.subs = self.observed.subscribe( function(newVal) {
    self.chart.addData(newVal.dataArray, newVal.label );
  });

  self.dispose = function() {
    self.subs.dispose();
  };

  var canvas = element.childNodes[1];
  canvas.width = self.width;
  canvas.height = self.height;
  var ctx = canvas.getContext("2d");

  self.chart = new Chart(ctx).Line({
    labels: [],
    datasets: [
        {
            label: "Messages/minute",
            fillColor: "rgba(151,187,205,0.2)",
            strokeColor: "rgba(151,187,205,1)",
            pointColor: "rgba(151,187,205,1)",
            pointStrokeColor: "#fff",
            pointHighlightFill: "#fff",
            pointHighlightStroke: "rgba(151,187,205,1)",
            data: []
        }
    ]
  });

}

ko.components.register('samplechart', {
    viewModel: {
      createViewModel: function(params, componentInfo) {
          // - 'params' is an object whose key/value pairs are the parameters
          //   passed from the component binding or custom element
          // - 'componentInfo.element' is the element the component is being
          //   injected into. When createViewModel is called, the template has
          //   already been injected into this element, but isn't yet bound.
          // - 'componentInfo.templateNodes' is an array containing any DOM
          //   nodes that have been supplied to the component.

          // Return the desired view model instance, e.g.:
          return new ChartModel(params,componentInfo.element);
        }
    },
    template: { element: 'sampletemplate' }
});