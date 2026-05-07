// Bouncing-ball reference sketch.
// https://example.org/zita/examples/bouncing-ball
//
// Designed to satisfy the "minimum requirement" rules: user-defined class with
// an explicit constructor, arithmetic on variables, a loop, a conditional, a
// boolean operator, an event handler, an array, a non-void function, and a
// parameterised method. Use it as a baseline when learning what a "clean"
// student-style sketch looks like to Zita.

Ball[] balls = new Ball[5];

void setup() {
  size(400, 400);
  for (int i = 0; i < balls.length; i = i + 1) {
    balls[i] = new Ball(random(width), random(height), randomSpeed());
  }
}

void draw() {
  background(240);
  for (int i = 0; i < balls.length; i = i + 1) {
    balls[i].update();
    balls[i].display();
  }
}

void mousePressed() {
  for (int i = 0; i < balls.length; i = i + 1) {
    balls[i].nudge(mouseX, mouseY);
  }
}

float randomSpeed() {
  return random(-3, 3);
}

class Ball {
  float x;
  float y;
  float speed;

  Ball(float startX, float startY, float startSpeed) {
    x = startX;
    y = startY;
    speed = startSpeed;
  }

  void update() {
    x = x + speed;
    if (x < 0 || x > width) {
      speed = -speed;
    }
  }

  void display() {
    fill(50, 120, 220);
    ellipse(x, y, 30, 30);
  }

  void nudge(float targetX, float targetY) {
    speed = speed + (targetX - x) * 0.01;
  }
}
