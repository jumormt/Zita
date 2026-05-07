int x = 10;
int y = 20;

void setup() {
  size(400, 400);
  background(#FF0000);
  fill(0);
  ellipse(x, y, 50, 50);
}

void draw() {
  x = x + 1;
}
