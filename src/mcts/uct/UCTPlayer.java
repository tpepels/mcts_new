package mcts.uct;

import framework.AIPlayer;
import framework.IBoard;
import framework.MoveCallback;
import framework.Options;
import mcts.TransposTable;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.List;

public class UCTPlayer implements AIPlayer {
    public UCTNode root;
    public IBoard board;
    private MoveCallback moveCallback;
    private TransposTable tt = new TransposTable();
    private int[] bestMove;
    private Options options;

    @Override
    public void getMove(IBoard board) {
        System.gc();
        if (options == null)
            throw new RuntimeException("MCTS Options not set.");
        root = new UCTNode(board.getPlayerToMove(), options, board.hash(), tt);
        int simulations = 0, nSamples = options.nSamples;
        long startT = System.currentTimeMillis();
        options.resetMAST(board.getMaxMoveId());

        if (!options.fixedSimulations) {
            long endTime = System.currentTimeMillis() + options.nSimulations;
            // Run the MCTS algorithm while time allows it
            while (true) {

                // Reset nSamples when doing heurisic resampling
                if(options.resample)
                    options.nSamples = nSamples;

                simulations += options.nSamples;

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

                // Reset nSamples when doing heurisic resampling
                if(options.resample)
                    options.nSamples = nSamples;

                simulations += options.nSamples;

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
            System.out.println("-------- < uct debug > ----------");
            for (UCTNode uctNode : root.children) {
                if (uctNode == bestChild)
                    System.out.print("====>> ");
                System.out.println(uctNode);
            }
            System.out.println("- player " + board.getPlayerToMove());
            System.out.println("- best child: " + bestChild.toString(board));
            System.out.println("- # of playouts: " + simulations);
            System.out.print("- searched for: " + ((endT - startT) / 1000.) + " sec. ");
            System.out.println((int) Math.round((1000. * simulations) / (endT - startT)) + " ppsec.");
            System.out.println("- collisions: " + tt.collisions + ", tps: " + tt.positions);
            System.out.println("-------- </uct debug > ----------");

            if (options.regression) {
                XYChart chart = getScatterPlot(bestChild.timeSeries,
                        bestChild.state.shortRegression, bestChild.state.longRegression,
                        bestChild.toString());
                new SwingWrapper<>(chart).displayChart();
            }

        }
        this.root = null;
        this.board = null;
        System.gc();
        if (moveCallback != null)
            moveCallback.makeMove(bestMove);
    }

    private XYChart getScatterPlot(List<Double> yData, SimpleRegression shortRegression, SimpleRegression longRegression, String name) {
        XYChart chart = new XYChartBuilder().width(800).height(600).build();

        // Customize Chart
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(2);

        int n = yData.size();
        // Series
        List<Integer> xData = new ArrayList<>();
        List<Double> rData = new ArrayList<>(), lData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            xData.add(i);
            rData.add(0, shortRegression.predict(n - i));
            lData.add(0, longRegression.predict(n - i));
        }
        for (int i = 0; i < 25; i++) {
            xData.add(1000 + i);
            rData.add(shortRegression.predict(n + i));
            lData.add(longRegression.predict(n + i));
        }

        chart.addSeries(name, xData.subList(0, 1000), yData.subList(n - 1000, n)).
                setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.addSeries("Short regression", xData, rData);
        chart.addSeries("Long regression", xData, lData);
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

