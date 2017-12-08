package org.nustaq.kontraktor.webapp.javascript.clojure;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.webapp.javascript.JSPostProcessor;
import java.io.IOException;
import java.util.ArrayList;

/**
 * uses blocking IO only applicable for build steps or build triggering initial request
 */
public class ClojureJSPostProcessor implements JSPostProcessor {

    @Override
    public String postProcess(String currentJS, JSPostProcessorContext context) {
        try {
            Compiler compiler = new Compiler();

            CompilerOptions options = new CompilerOptions();
            options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);

            // Advanced mode is used here, but additional options could be set, too.
            CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

            // To get the complete set of externs, the logic in
            // CompilerRunner.getDefaultExterns() should be used here.
            SourceFile extern = SourceFile.fromCode("xxxx.js", "");

            // The dummy input name "input.js" is used here so that any warnings or
            // errors will cite line numbers in terms of input.js.
            SourceFile input = SourceFile.fromCode("input.js", currentJS);

            ArrayList in = new ArrayList();
            in.add(input);
            // compile() returns a Result, but it is not needed here.
            Result compile = compiler.compile(CommandLineRunner.getBuiltinExterns(CompilerOptions.Environment.BROWSER), in, options);
//            System.out.println(compile);

            // The compiler is responsible for generating the compiled code; it is not
            // accessible via the Result.
            String s = compiler.toSource();
//            System.out.println(s);
            return s;
        } catch (Exception e) {
            Log.Warn(this, e);
        }
        return currentJS;
    }


    public static void main(String[] args) {
        ClojureJSPostProcessor p = new ClojureJSPostProcessor();
        p.postProcess("(new function() {\n" +
            "  let React = null;\n" +
            "  let Component = null;\n" +
            "  const _initmods = () => {\n" +
            "    React = _kresolve('react/index');\n" +
            "    Component = _kresolve('react/index', 'Component');\n" +
            "  };\n" +
            "  kaddinit(_initmods);\n" +
            "\n" +
            "const WEEKDAYS = [\"Sonntag\",\"Montag\",\"Dienstag\",\"Mittwoch\",\"Donnerstag\",\"Freitag\", \"Samstag\"];\n" +
            "const WEEKDAYS_SHORT = [\"So\",\"Mo\",\"Di\",\"Mi\",\"Do\",\"Fr\", \"Sa\"];\n" +
            "const MONTHS = [\"Januar\",\"Februar\",\"März\",\"April\",\"Mai\",\"Juni\",\"Juli\",\"August\",\"September\",\"Oktober\",\"November\",\"Dezember\"];\n" +
            "const MONTHS_SHORT = [\"Jan\",\"Feb\",\"Mar\",\"Apr\",\"Mai\",\"Jun\",\"Jul\",\"Aug\",\"Sep\",\"Okt\",\"Nov\",\"Dez\"];\n" +
            "\n" +
            "class DateUtilities {\n" +
            "  \n" +
            "  calcAge(birthday) {\n" +
            "    return new Date().getYear() - birthday.getYear();\n" +
            "  }\n" +
            "  \n" +
            "  getWeekDayShort(date) {\n" +
            "    return WEEKDAYS_SHORT[date.getDay()];\n" +
            "  }\n" +
            "  \n" +
            "  getWeekDay(date) {\n" +
            "    return WEEKDAYS[date.getDay()];\n" +
            "  }\n" +
            "  \n" +
            "  getMonthShort(date) {\n" +
            "    return MONTHS_SHORT[date.getMonth()];\n" +
            "  }\n" +
            "  \n" +
            "  dayMillis() {\n" +
            "    return 1000*60*60*24;\n" +
            "  }\n" +
            "  \n" +
            "  weekMillis() {\n" +
            "    return this.dayMillis()*7;\n" +
            "  }\n" +
            "  \n" +
            "  /**\n" +
            "  * set time of a date to 00:00:00\n" +
            "  * @param date\n" +
            "  * @returns {Date}\n" +
            "  */\n" +
            "  normalizeToDay(date) {\n" +
            "    const copy = new Date(date);\n" +
            "    copy.setHours(0);\n" +
            "    copy.setMinutes(0);\n" +
            "    copy.setSeconds(0);\n" +
            "    copy.setMilliseconds(0);\n" +
            "    return copy;\n" +
            "  }\n" +
            "  \n" +
            "  normalizeToWeek(date) {\n" +
            "    let copy = new Date(date);\n" +
            "    while( copy.getDay() != 0 ) // monday\n" +
            "    copy = new Date(copy.getTime()-this.dayMillis());\n" +
            "    return this.normalizeToDay(copy);\n" +
            "  }\n" +
            "  \n" +
            "  normalizeToMonthFirst(date) {\n" +
            "    return new Date(date.getFullYear(), date.getMonth(), 1);\n" +
            "  }\n" +
            "  \n" +
            "  normalizeToMonthLast(date) {\n" +
            "    return new Date(date.getFullYear(), date.getMonth()+1, 1);\n" +
            "  }\n" +
            "  \n" +
            "  getDaysInMonth(date) {\n" +
            "    return new Date(date.getYear()+1900,date.getMonth()+1,0).getDate();\n" +
            "  }\n" +
            "  \n" +
            "  normalizeToWeekEnd(date) {\n" +
            "    return this.normalizeToWeek(date.getTime()+this.weekMillis());\n" +
            "  }\n" +
            "  \n" +
            "  getShortDateString(date) {\n" +
            "    return date.getDate()+\".\"+(date.getMonth()+1)+\".\";\n" +
            "  }\n" +
            "  \n" +
            "  getPreviousMonth(date) {\n" +
            "    const res = new Date(date.getTime());\n" +
            "    const m = date.getMonth()-1;\n" +
            "    if ( m < 0 ) {\n" +
            "      res.setYear(1900+res.getYear()-1);\n" +
            "      res.setMonth(11);\n" +
            "      return res;\n" +
            "    }\n" +
            "    res.setMonth(m);\n" +
            "    return res;\n" +
            "  }\n" +
            "  \n" +
            "  getWeekOfYear(d) {\n" +
            "    // stackoverflow code\n" +
            "    \n" +
            "    // Copy date so don't modify original\n" +
            "    d = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()));\n" +
            "    // Set to nearest Thursday: current date + 4 - current day number\n" +
            "    // Make Sunday's day number 7\n" +
            "    d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay()||7));\n" +
            "    // Get first day of year\n" +
            "    var yearStart = new Date(Date.UTC(d.getUTCFullYear(),0,1));\n" +
            "    // Calculate full weeks to nearest Thursday\n" +
            "    var weekNo = Math.ceil(( ( (d - yearStart) / 86400000) + 1)/7);\n" +
            "    return weekNo;\n" +
            "  }\n" +
            "  \n" +
            "  getDateString(d){\n" +
            "    if( d )\n" +
            "    {\n" +
            "      let day = d.getDate();\n" +
            "      let month = d.getMonth() + 1;\n" +
            "      let year = d.getFullYear();\n" +
            "      return (day < 10 ? \"0\"+day : day) + \".\" + (month < 10 ? \"0\"+month : month ) + \".\" + year ;\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  getTimeString(d){\n" +
            "    if( d )\n" +
            "    {\n" +
            "      var hours = d.getHours();\n" +
            "      var minutes = d.getMinutes();\n" +
            "      var seconds = d.getSeconds();\n" +
            "      minutes = minutes < 10 ? '0'+minutes : minutes;\n" +
            "      seconds = seconds < 10 ? '0'+seconds : seconds;\n" +
            "      var strTime = hours + ':' + minutes+\":\"+seconds;\n" +
            "      return strTime;\n" +
            "    }\n" +
            "  }\n" +
            "  \n" +
            "  dayAfter(date , noOfDays){\n" +
            "    const addMillis = noOfDays * 1000 * 60 * 60 * 24;\n" +
            "    return new Date(date.getTime() + addMillis);\n" +
            "  }\n" +
            "  dayBefore(date , noOfDays){\n" +
            "    const minusMillis = noOfDays * 1000 * 60 * 60 * 24;\n" +
            "    return new Date(date.getTime() - minusMillis);\n" +
            "  }\n" +
            "  \n" +
            "  \n" +
            "  \n" +
            "  \n" +
            "}\n" +
            "\n" +
            "class MeduUtilities {\n" +
            "  \n" +
            "  /**\n" +
            "  *\n" +
            "  * @param weight\n" +
            "  * @param height\n" +
            "  * @returns {number}\n" +
            "  */\n" +
            "  calcBMI(weight, height ){\n" +
            "    return weight / ((height / 100) * (height / 100));\n" +
            "  }\n" +
            "  \n" +
            "  /**\n" +
            "  * @param {User} user\n" +
            "  * @returns {number} GU * PAL - 500\n" +
            "  */\n" +
            "  calculateGU(user) {\n" +
            "    let {_weight, _height, sex, birthday, target} = user,\n" +
            "    weight = _weight,\n" +
            "    height = _height,\n" +
            "    pal = 1.2,\n" +
            "    bmi = this.calcBMI(weight, height),\n" +
            "    age = DateUtil.calcAge(birthday),\n" +
            "    ree = 0, // Grundumsatz Ruheenergie\n" +
            "    gu = 0, // Grundumsatz inkl. Alltagsaktivitäten,\n" +
            "    result = 0, // Gesamtkalorien ggf. abzgl 500kCal zur Reduktionskost\n" +
            "    reduceCal = UserUtilities.isWeightTargetActive(user) || UserUtilities.isWaistTargetActive(user);\n" +
            "    \n" +
            "    sex = sex == \"f\" ? 0 : 1;\n" +
            "    \n" +
            "    if (bmi <= 18.5) {\n" +
            "      ree = (0.07122 * weight) - (0.02149 * age) + (0.82 * sex) + 0.731;\n" +
            "    }\n" +
            "    else if (bmi > 18.5 && bmi <= 25) {\n" +
            "      ree = (0.02219 * weight) + (0.02118 * height) + (0.884 * sex) - (0.01191 * age) + 1.233;\n" +
            "    }\n" +
            "    else if (bmi > 25 && bmi < 30) {\n" +
            "      ree = (0.04507 * weight) + (1.006 * sex) - (0.01553 * age) + 3.407;\n" +
            "    }\n" +
            "    else if (bmi >= 30) {\n" +
            "      ree = (0.05 * weight) + (1.103 * sex) - (0.01586 * age) + 2.924;\n" +
            "    }\n" +
            "    ree = ree / 4.187 * 1000; // MJ -> kCal\n" +
            "    gu = ree * pal; // PAL\n" +
            "    result = reduceCal ? gu - 500 : gu; // ggf. Reduktionskost\n" +
            "    result = result < ree ? ree : result;\n" +
            "    \n" +
            "    return result;\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "class UserUtilities {\n" +
            "  static isWeightTargetActive(user){\n" +
            "    return UserUtilities._isTargetActive(user._weight, user.target.weightLossTargetKG)\n" +
            "    && !UserUtilities._isTargetExpired(user.target.weightLossTargetTimeMS)\n" +
            "  }\n" +
            "  \n" +
            "  static isWaistTargetActive(user){\n" +
            "    return UserUtilities._isTargetActive(user._waist, user.target.waistLossTargetCM)\n" +
            "    && !UserUtilities._isTargetExpired(user.target.waistLossTargetTimeMS)\n" +
            "  }\n" +
            "  \n" +
            "  static _isTargetActive(currentVal, targetVal){\n" +
            "    return targetVal > 0 && currentVal > targetVal;\n" +
            "  }\n" +
            "  \n" +
            "  static _isTargetExpired(targetTime){\n" +
            "    let now = new Date();\n" +
            "    return now.getTime() > targetTime;\n" +
            "  }\n" +
            "  \n" +
            "}\n" +
            "\n" +
            "class Utilities {\n" +
            "  \n" +
            "  randomNum(min, max){\n" +
            "    let rand = (Math.random() * (max - min) + min);\n" +
            "    return rand;\n" +
            "  }\n" +
            "  \n" +
            "  \n" +
            "  roundToTwo(num) {\n" +
            "    return Math.round(num , 2) ;\n" +
            "  }\n" +
            "  \n" +
            "  scrollTo(rootElement, elementId){\n" +
            "    let scrollToElement = document.getElementById(elementId);\n" +
            "    // let height = rootElement.scrollHeight;\n" +
            "    // let rect = scrollToElement.getBoundingClientRect();\n" +
            "    // let rootRect = rootElement.getBoundingClientRect();\n" +
            "    // FIXME : Wrong calculation ..\n" +
            "    let to = scrollToElement.offsetTop - rootElement.offsetTop;\n" +
            "    this._scroll(rootElement, to , 200);\n" +
            "  }\n" +
            "  \n" +
            "  \n" +
            "  _scroll(element, to, duration) {\n" +
            "    var start = element.scrollTop,\n" +
            "    change = to - start,\n" +
            "    currentTime = 0,\n" +
            "    increment = 10;\n" +
            "    \n" +
            "    var animateScroll = function(){\n" +
            "      currentTime += increment;\n" +
            "      var val = DevUtil._easeInOutQuad(currentTime, start, change, duration);\n" +
            "      element.scrollTop = val;\n" +
            "      if(currentTime < duration) {\n" +
            "        setTimeout(animateScroll, increment);\n" +
            "      } else {\n" +
            "        console.log(\"SCROLLTO \"+to);\n" +
            "        element.scrollTop = to;\n" +
            "      }\n" +
            "    };\n" +
            "    animateScroll();\n" +
            "  }\n" +
            "  \n" +
            "  _easeInOutQuad (t, b, c, d) {\n" +
            "    t /= d/2;\n" +
            "    if (t < 1) return c/2*t*t + b;\n" +
            "    t--;\n" +
            "    return -c/2 * (t*(t-2) - 1) + b;\n" +
            "  }\n" +
            "  \n" +
            "}\n" +
            "\n" +
            "function offset(el) {\n" +
            "  var rect = el.getBoundingClientRect(),\n" +
            "  scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,\n" +
            "  scrollTop = window.pageYOffset || document.documentElement.scrollTop;\n" +
            "  return { top: rect.top + scrollTop, left: rect.left + scrollLeft }\n" +
            "}\n" +
            "\n" +
            "class HCenter extends Component {\n" +
            "  \n" +
            "  render() {\n" +
            "    const style = _sprd({\n" +
            "        alignContent: 'center',\n" +
            "        alignItems: 'center',\n" +
            "        boxSizing: 'border-box',\n" +
            "        display: 'flex',\n" +
            "        flexDirection: 'row',\n" +
            "        flexWrap: 'nowrap',\n" +
            "        justifyContent: 'center',\n" +
            "        '...0':this.props.style\n" +
            "      });\n" +
            "    return (React.createElement(\n" +
            "        'div',\n" +
            "        {\n" +
            "          'style': style \n" +
            "        },\n" +
            "        this.props.children ))\n" +
            "  }\n" +
            "}\n" +
            "\n" +
            "\n" +
            "\n" +
            "const MeduUtil = new MeduUtilities();\n" +
            "const DateUtil = new DateUtilities();\n" +
            "const DevUtil = new Utilities();\n" +
            "\n" +
            "\n" +
            "  kimports['common/util'] = {};\n" +
            "  kimports['common/util'].WEEKDAYS = WEEKDAYS;\n" +
            "  kimports['common/util'].WEEKDAYS_SHORT = WEEKDAYS_SHORT;\n" +
            "  kimports['common/util'].MONTHS = MONTHS;\n" +
            "  kimports['common/util'].MONTHS_SHORT = MONTHS_SHORT;\n" +
            "  kimports['common/util'].DateUtilities = DateUtilities;\n" +
            "  kimports['common/util'].MeduUtilities = MeduUtilities;\n" +
            "  kimports['common/util'].UserUtilities = UserUtilities;\n" +
            "  kimports['common/util'].Utilities = Utilities;\n" +
            "  kimports['common/util'].offset = offset;\n" +
            "  kimports['common/util'].HCenter = HCenter;\n" +
            "  kimports['common/util'].MeduUtil = MeduUtil;\n" +
            "  kimports['common/util'].DateUtil = DateUtil;\n" +
            "  kimports['common/util'].DevUtil = DevUtil;\n" +
            "  kimports['common/util'].__kdefault__ = DevUtil;\n" +
            "});", null);
    }
}
