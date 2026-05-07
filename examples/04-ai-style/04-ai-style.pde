// ================ GLOBAL VARIABLES ================
// Author: [Your Name]
// Reference: https://en.wikipedia.org/wiki/Pong [cite: 4, 7]
//
// 3 seconds at 60 fps before the ball resets

int x = 100;
int dir = random(1) > 0.5 ? 1 : -1;
int counter = 0;

void setup() {
  size(400, 400);
}

void draw() {
  background(0);  // black background each frame
  fill(255);      // white fill
  ellipse(x, 200, 20, 20);  // ellipse at center position
  if (random(1) < 0.01) dir *= -1;
  x += dir;
  counter += 1;
}

void resetCounter() {
}
