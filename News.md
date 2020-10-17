#PlainDict 2.0
- Virtual Index (VI). 
Different from actual indexes, VIs don't have actual content to display directly. Instead, I use VIs to point to actual indexes, and use the associated json content to update the actual content.
VIs are displayed overwhelming the actual indexes.
Below give the format of VIs' content:
{
ai:0, //actual index it's pointing to.
js:"",//js content to inject.
css:""//css content to inject.
}


#PlainDict 1.0
- pdf:// tag

#legacy
- entry:// tag