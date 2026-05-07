// Multi-file sketch to demonstrate Zita's project-level concatenation.
// https://example.org/zita/examples/multi-file
//
// Zita loads every .pde file in a sketch directory and analyses them as one
// synthetic Java compilation unit, so a class declared in Particle.pde is
// visible from the main file with no import.

Particle[] particles = new Particle[10];

void setup() {
  size(500, 500);
  for (int i = 0; i < particles.length; i = i + 1) {
    particles[i] = new Particle(width / 2, height / 2);
  }
}

void draw() {
  background(20);
  for (int i = 0; i < particles.length; i = i + 1) {
    particles[i].step();
    particles[i].render();
  }
}

void keyPressed() {
  for (int i = 0; i < particles.length; i = i + 1) {
    particles[i].reset(width / 2, height / 2);
  }
}
