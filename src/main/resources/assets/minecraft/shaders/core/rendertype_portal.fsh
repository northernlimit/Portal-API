#version 150

uniform sampler2D MainColor;
uniform sampler2D MainDepth;
uniform sampler2D PortalColor;
uniform sampler2D MaskDepth;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    float mainDepth = texture(MainDepth, texCoord).r;
    float portalDepth = texture(MaskDepth, texCoord).r;
    if (mainDepth < portalDepth) {
        fragColor = texture(MainColor, texCoord);
    } else {
        fragColor = texture(PortalColor, texCoord);
    }
}