class Particle {
  float px;
  float py;
  float vx;
  float vy;

  Particle(float startX, float startY) {
    reset(startX, startY);
  }

  void reset(float startX, float startY) {
    px = startX;
    py = startY;
    vx = random(-2, 2);
    vy = random(-2, 2);
  }

  void step() {
    px = px + vx;
    py = py + vy;
    if (px < 0 || px > width) {
      vx = -vx;
    }
    if (py < 0 || py > height) {
      vy = -vy;
    }
  }

  void render() {
    fill(200, 200, 100);
    noStroke();
    ellipse(px, py, 6, 6);
  }
}
