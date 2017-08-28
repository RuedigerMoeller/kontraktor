import React from 'react';

const Static = () => {
  return (
    <div>
      <h3>Example showcasing ..</h3>
      <ul>
        <li>kontraktors <b>.jsx</b> transpiler</li>
        <li>kontraktors <b>npm clone</b> ("JNPM")</li>
        <li><b>no nodejs</b> required</li>
        <li><b>Session handling</b></li>
        <li>Session <b>timeout</b> handling</li>
        <li>Session <b>resurrection</b> handling (SPA client was away/offline and connects back)</li>
        <li><b>Dynamic connection type</b> (http adaptive long poll or websockets) without code change</li>
        <li>Set DEVMODE in ReactMaterialUITestApp to false to get production-level-<b>bundling+optimization</b> </li>
      </ul>
    </div>
  )
};

export default Static;