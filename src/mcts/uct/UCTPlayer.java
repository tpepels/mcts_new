package mcts.uct;

import framework.AIPlayer;
import framework.IBoard;
import framework.MoveCallback;
import framework.Options;
import mcts.TransposTable;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.List;

public class UCTPlayer implements AIPlayer {
    private MoveCallback moveCallback;
    private TransposTable tt = new TransposTable();
    private int[] bestMove;
    private Options options;
    public UCTNode root;
    public IBoard board;

    @Override
    public void getMove(IBoard board) {
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        root = new UCTNode(board.getPlayerToMove(), options, board.hash(), tt);
        int simulations = 0;
        long startT = System.currentTimeMillis();
        options.resetMAST(board.getMaxMoveId());
        if (!options.fixedSimulations) {
            long endTime = System.currentTimeMillis() + options.time;
            // Run the MCTS algorithm while time allows it
            while (true) {
                simulations++;
                if (System.currentTimeMillis() >= endTime)
                    break;
                if (Options.debug)
                    options.checkRaveMoves();
                options.resetRAVE(board.getMaxMoveId());
                // Make one simulation from root to leaf.
                root.MCTS(board.clone(), 0);
                // Check if the root is proven
                if (root.isSolved())
                    break; // Break if you find a winning move
            }
        } else {
            // Run as many simulations as allowed
            while (simulations < options.nSimulations) {
                simulations++;
                if (Options.debug)
                    options.checkRaveMoves();
                options.resetRAVE(board.getMaxMoveId());
                // Make one simulation from root to leaf.
                root.MCTS(board.clone(), 0);

                if (root.isSolved())
                    break; // Break if you find a winning move
            }
        }
        long endT = System.currentTimeMillis();
        // Return the best move found
        UCTNode bestChild = root.getBestChild();
        bestMove = bestChild.move;
        // Pack the transpositions
        int removed = tt.pack(1);

        // show information on the best move
        if (Options.debug) {
            System.out.println("-------- < UCT Debug > ----------");
            System.out.println("- Player " + board.getPlayerToMove());
            System.out.println("- Best child: " + bestChild.toString(board));
            System.out.println("- Play-outs: " + simulations);
            System.out.println("- Searched for: " + ((endT - startT) / 1000.) + " s.");
            System.out.println("- " + (int) Math.round((1000. * simulations) / (endT - startT)) + " playouts per s");
            System.out.println("- Pack cleaned: " + removed + " transpositions");
            for (UCTNode uctNode : root.children) {
                if (uctNode == bestChild)
                    System.out.print("====>> ");
                System.out.println(uctNode);
            }
            System.out.println("-------- </UCT Debug > ----------");
//            if(bestChild.state.simpleRegression != null) {
//                XYChart chart = getScatterPlot(bestChild.timeSeries, bestChild.state.simpleRegression, bestChild.toString());
//                new SwingWrapper<XYChart>(chart).displayChart();
//                try {
//                    Thread.sleep(10000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
        }
        // Set the root to the best child, so in the next move, the opponent's move can become the new root
        root = null;
        this.board = null;

        if (moveCallback != null)
            moveCallback.makeMove(bestMove);
    }

    private XYChart getScatterPlot(List<Double> yData, SimpleRegression simpleRegression, String name) {
        XYChart chart = new XYChartBuilder().width(800).height(600).build();

        // Customize Chart
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(2);

        int n = yData.size();
        // Series
        List<Integer> xData = new ArrayList<>();
        List<Double> rData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            xData.add(i);
            rData.add(0, simpleRegression.predict(n - i));
        }

        chart.addSeries(name, xData, yData.subList(n - 1000, n)).
                setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.addSeries("Regression", xData, rData);
        return chart;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    @Override
    public int[] getBestMove() {
        return bestMove;
    }

    public void setMoveCallback(MoveCallback moveCallback) {
        this.moveCallback = moveCallback;
    }

    public void setBoard(IBoard board) {
        this.board = board;
    }

    @Override
    public void run() {
        assert board != null : "Set the board first!";
        getMove(this.board);
    }
}

