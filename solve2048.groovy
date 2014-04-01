@Grapes([
@Grab("org.gebish:geb-core:0.9.2"),
@Grab("org.seleniumhq.selenium:selenium-chrome-driver:2.33.0"),
@Grab("org.seleniumhq.selenium:selenium-support:2.26.0")
])
import geb.Browser
import org.openqa.selenium.Keys
import org.openqa.selenium.chrome.ChromeDriver

def getBoard(browser) {
  def tileClass = 'tile-position-'
  int[][] res = new int[4][4]
  try {
    def board = browser.$('div.tile', class: browser.startsWith(tileClass))
    board.each { elem ->
      def indexes = elem.classes().find {it.startsWith(tileClass)}.substring(tileClass.size()).split('-')
      res[indexes[0].toInteger() - 1][indexes[1].toInteger() - 1] = elem.text().toInteger()
    }
  } catch (Exception e) {
    //nothing
  }
  res
}

def smoothness(board) {
  int weight = 0
  for (int i = 0; i < 4; i++) {
    for (int j = 0; j < 4; j++) {
      // calculate abs for right and down
      if (i != 3) {
        weight += Math.abs(board[i][j] - board[i+1][j])
      }
      if (j != 3) {
        weight += Math.abs(board[i][j] - board[i][j+1])
      }
    }
  }
  -weight
}

def space(board) {
  int weight = 0
  for (int i = 0; i < 4; i++) {
    for (int j = 0; j < 4; j++) {
      if (board[i][j] == 0) {
        weight += 100
      }
    }
  }
  weight
}

def mono(board) {
  int weight = 0
  4.times {
    if (straightline(board[it])) {
      weight += 100
    }
    if (straightline((0..3).collect { j -> board[j][it] })) {
      weight += 100
    }
  }
  weight
}

// either inc or dec
def straightline(line) {
  boolean inc = line[0] < line[1]
  for (int i = 1; i < 3; i++) {
    if (inc && line[i] > line[i+1]) {
      return false
    } else if (!inc && line[i] < line[i+1]) {
      return false
    }
  }
  return true
}

def calculateWeight(board) {
  smoothness(board) + space(board) * 10 + mono(board) * 5
}

def getWeight(board, depth) {
  (0..3).collect {
    def newBoard = move(board, it)
    if (newBoard == board) {
      // exclude move that doesn't change the board
      return -999999
    } else {
      return minimax(newBoard, depth)
    }
  }
}

def minimax(board, depth) {
  if (depth == 0) {
    return calculateWeight(board)
  }

  int[][] copy = copyBoard(board)
  def max = -999999
  if (!full(board)) {
    for (int i = 0; i < 4; i++) {
      for (int j = 0; j < 4; j++) {
        if (copy[i][j] == 0) {
          copy[i][j] = 2
          4.times {
            def weight = minimax(move(copy, it), depth - 1)
            if (weight > max) {
              max = weight
            }
          }
          copy = copyBoard(board)
        }
      }
    }
  } else {
    4.times {
      def weight = minimax(move(copy, it), depth - 1)
      if (weight > max) {
        max = weight
      }
    }
  }
  return max
}

// move line to left
int[] moveLine(int[] source) {
  int gridSize=4
  int len = source.length
  int[] res = new int[len]
  int idx = 0
  for (int i=0;i< len;i++) {
    if (source[i]!=0) {
      res[idx++] = source[i];
    }
  }
  int start = 0
  int next = 0
  double score = 0d
  while (start < len) {
    def ps = next>gridSize-1?0:res[next]
    def pv = next>gridSize-2?0:res[next + 1]
    if (ps == pv) {
      res[start] = 2 * ps
      score += 4*ps*ps
      next++
    } else {
      res[start] = ps
    }
    next++
    start++
  }
  res
}

int[][] copyBoard(board) {
  int[][] res = new int[4][4]
  for (int i = 0; i < 4; i++) {
    for (int j = 0; j < 4; j++) {
      res[i][j] = board[i][j]
    }
  }
  res
}

int[][] move(orig, direction) {
  int[][] board = copyBoard(orig)
  switch (direction) {
    case 0: //up j-1
      for (int i = 0; i < 4; i++) {
        board[i] = moveLine(board[i])
      }
      break
    case 1: //down j+1
      for (int i = 0; i < 4; i++) {
        board[i] = (moveLine((board[i] as List).reverse() as int[]) as List).reverse() as int[]
      }
      break
    case 2: //left i-1
      for (int i = 0; i < 4; i++) {
        def line = [board[0][i], board[1][i], board[2][i], board[3][i]] as int[]
        line = moveLine(line)
        4.times {
          board[it][i] = line[it]
        }
      }
      break
    case 3: //right i+1
      for (int i = 0; i < 4; i++) {
        def line = [board[0][i], board[1][i], board[2][i], board[3][i]].reverse() as int[]
        line = moveLine(line)
        line = (line as List).reverse() as int[]
        4.times {
          board[it][i] = line[it]
        }
      }
      break
  }
  board
}

def bestNext(board) {
  def weightList = getWeight(board, 2)
//    println "weightList: ${weightList}"
  weightList.indexOf(weightList.max())
}

def outputBoard(board) {
  println "----------"
  4.times { i ->
    4.times { j ->
      print "${board[j][i]}\t"
    }
    println ""
  }
  println "----------"
}

def gameOver(board) {
  (0..3).every {
    board == move(board, it)
  }
}

def full(board) {
  for (int i = 0; i < 4; i++) {
    for (int j = 0; j < 4; j++) {
      if (board[i][j] == 0) return false
    }
  }
  return true
}

def outputNextMove(nextMove) {
  switch (nextMove) {
    case 0:
      println "↑"
      break
    case 1:
      println "↓"
      break
    case 2:
      println "←"
      break
    case 3:
      println "→"
      break
  }
}

browser = new Browser(driver: new ChromeDriver())

browser.with {
  go "http://gabrielecirulli.github.io/2048/"

  def board
  while (true) {
    if ($('div.game-over')) {
      println "Score: ${$('div.score-container').text()} ${board}"
      $('a.retry-button').click()
    }

    board = getBoard(browser)
    def nextMove = bestNext(board)
    switch (nextMove) {
      case 0:
        $('body')<<Keys.UP
        break
      case 1:
        $('body')<<Keys.DOWN
        break
      case 2:
        $('body')<<Keys.LEFT
        break
      case 3:
        $('body')<<Keys.RIGHT
        break
    }
  }
}
