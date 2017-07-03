// quirksy textual import shim (see JSXTRanspiler) to overcome the babel, commonjs, amd, import/export hell
"#include login.jsx";
"#include fbreactexample.jsx";

ReactDOM.render(<div><Game /><Login /></div>, document.getElementById("root"));
