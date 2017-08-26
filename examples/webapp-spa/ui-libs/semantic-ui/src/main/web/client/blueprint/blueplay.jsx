import {Component} from 'react';
import {Core} from 'blueprintjs/blueprintjs.js'

class BluePlay extends Component {

  constructor(props) {
    super(props);
    const tooltipLabel = <Core.Tooltip content="An eye!"><span className="pt-icon-standard pt-icon-eye-open"/></Core.Tooltip>;
    const longLabel = "Organic meditation gluten-free, sriracha VHS drinking vinegar beard man.";
    /* tslint:disable:object-literal-sort-keys so childNodes can come last */
    this.state = {
      nodes: [
        {
          hasCaret: true,
          iconName: "folder-close",
          label: "Folder 0",
        },
        {
          iconName: "folder-close",
          isExpanded: true,
          label: <Core.Tooltip content="I'm a folder <3">Folder 1</Core.Tooltip>,
          childNodes: [
            { iconName: "document", label: "Item 0", secondaryLabel: tooltipLabel },
            { iconName: "pt-icon-tag", label: longLabel },
            {
              hasCaret: true,
              iconName: "pt-icon-folder-close",
              label: <Core.Tooltip content="foo">Folder 2</Core.Tooltip>,
              childNodes: [
                { label: "No-Icon Item" },
                { iconName: "pt-icon-tag", label: "Item 1" },
                {
                  hasCaret: true, iconName: "pt-icon-folder-close", label: "Folder 3",
                  childNodes:  [
                    { iconName: "document", label: "Item 0" },
                    { iconName: "pt-icon-tag", label: "Item 1" },
                  ],
                },
              ],
            },
          ],
        },
      ],
    };
    var i = 1;
    this.forEachNode(this.state.nodes, (n) => n.id = i++);
  }

  // override @PureRender because nodes are not a primitive type and therefore aren't included in
  // shallow prop comparison
  shouldComponentUpdate() {
    return true;
  }

  handleNodeClick(nodeData,_nodePath, e) {
    const originallySelected = nodeData.isSelected;
    if (!e.shiftKey) {
      this.forEachNode(this.state.nodes, (n) => n.isSelected = false);
    }
    nodeData.isSelected = originallySelected == null ? true : !originallySelected;
    this.setState(this.state);
  }

  handleNodeCollapse(nodeData) {
    nodeData.isExpanded = false;
    this.setState(this.state);
  }

  handleNodeExpand(nodeData) {
    nodeData.isExpanded = true;
    this.setState(this.state);
  }

  forEachNode(nodes, callback) {
    if (nodes == null) {
      return;
    }

    for (const node of nodes) {
      callback(node);
      this.forEachNode(node.childNodes, callback);
    }
  }

  render() {
    return (
      <div>
        <h1>Blueprint</h1>
        <div style={{minWidth: 600}}>
          <Core.Tree
            contents={this.state.nodes}
            onNodeClick={ (nodeData,_nodePath, e) => this.handleNodeClick(nodeData,_nodePath, e) }
            onNodeCollapse={ (nodeData) => this.handleNodeCollapse(nodeData) }
            onNodeExpand={ (nodeData) => this.handleNodeExpand(nodeData) }
            className={Core.Classes.ELEVATION_0}
          />
        </div>
      </div>
    );
  }

}