#version 120

varying vec4 color;
varying vec2 uvcoord;

uniform sampler2D Diffuse;
uniform vec4 BiomeColor;

void main()
{
    vec4 fontsample = texture2D(Diffuse, uvcoord);
    gl_FragColor = fontsample * color * BiomeColor;
} 