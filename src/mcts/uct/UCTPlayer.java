package mcts.uct;

import framework.*;
import mcts.State;
import mcts.TransposTable;

import java.awt.*;
import java.io.File;
import java.io.IOException;

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
        int simulations = 0;
        double nSamples = options.nSamples;
        long startT = System.currentTimeMillis();
        options.resetMAST(board.getMaxMoveId());

        if (!options.fixedSimulations) {
            long endTime = System.currentTimeMillis() + options.nSimulations;
            // Run the MCTS algorithm while time allows it
            while (true) {

                // Reset nSamples when doing heurisic resampling
                if (options.resample)
                    options.nSamples = nSamples;

                simulations += options.nSamples;

                if (System.currentTimeMillis() >= endTime)
                    break;

                if (Options.debug && options.nSamples == 1)
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
                if (options.resample)
                    options.nSamples = nSamples;

                simulations += options.nSamples;

                if (Options.debug && options.nSamples == 1)
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


        // show information on the best move
        if (Options.debug) {
            System.out.println("-------- < uct debug > ----------");
            for (UCTNode uctNode : root.children) {
                if (uctNode == bestChild)
                    System.out.print("====>> ");
                System.out.println(uctNode.toString(board));
            }
            System.out.println("- player " + board.getPlayerToMove());
            System.out.println("- best child: " + bestChild.toString(board));
            System.out.println("- # of playouts: " + State.df0.format(simulations));
            System.out.print("- searched for: " + State.df0.format((endT - startT) / 1000.) + " sec. ");
            System.out.println(State.df0.format((int) Math.round((1000. * simulations) / (endT - startT))) + " ppsec.");
            System.out.println("- collisions: " + State.df0.format(tt.collisions) + ", tps: " + State.df0.format(tt.positions));
            System.out.println("- MaxEval: " + Options.maxEval);
            System.out.println("-------- </uct debug > ----------");
            if(options.regression) {
                cleanDir(new File("C:\\Users\\tpepe\\Desktop\\charts\\regressor\\"));
                cleanDir(new File("C:\\Users\\tpepe\\Desktop\\charts\\"));
                cleanDir(new File("C:\\Users\\tpepe\\Desktop\\charts\\cusum"));
                for (UCTNode c : root.children) {
                    CUSUMChangeDetector cSum = new CUSUMChangeDetector();
                    Plot.Data changePoints = Plot.data();
                    Plot.Data mean = Plot.data(), poscusum = Plot.data(), negcusum = Plot.data();;
                    for (int i = 0; i < c.timeSeries.size(); i++) {
                        double x = c.timeSeries.x(i);
                        if (cSum.update(c.timeSeries.y(i))) {
                            changePoints.xy(x, c.timeSeries.y(i));
                            cSum.reset();
                        }
                        mean.xy(x, cSum.getMean());
                        poscusum.xy(x, cSum.getPosCusum());
                        negcusum.xy(x, cSum.getNegCusum());
                    }
                    Plot.Data regressor = Plot.data();
                    double rs = c.getVisits() - c.state.regressor.getN(), re = c.getVisits() + 20;
                    for (int i = (int) rs; i < re; i++) {
                        regressor.xy(i, c.state.regressor.predict(i));
                    }


                    Plot plot = Plot.plot(Plot.plotOpts().title("Move plot").height(1080).width(1920)).
                            yAxis("mean", Plot.axisOpts().range(-1, 1)).
                            xAxis("time", Plot.axisOpts().range(rs, re).format(Plot.AxisFormat.NUMBER_INT))
                            .series("ChangePoint", changePoints, Plot.seriesOpts().marker(Plot.Marker.CIRCLE).line(Plot.Line.NONE).color(Color.RED))
                            .series("Mean Value", c.timeSeries, Plot.seriesOpts())
                            .series("Regressor", regressor, Plot.seriesOpts().line(Plot.Line.SOLID).color(Color.GREEN));
                    try {
                        plot.save("C:\\Users\\tpepe\\Desktop\\charts\\regressor\\plot_reg" + board.getMoveString(c.move), "png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Plot plot3 = Plot.plot(Plot.plotOpts().title("Move plot").height(1080).width(1920)).
                            yAxis("mean", Plot.axisOpts().range(-1, 1)).
                            xAxis("time", Plot.axisOpts().range(0, c.getVisits()).format(Plot.AxisFormat.NUMBER_INT))
                            .series("ChangePoint", changePoints, Plot.seriesOpts().marker(Plot.Marker.CIRCLE).line(Plot.Line.NONE).color(Color.RED))
                            .series("Mean Value", c.timeSeries, Plot.seriesOpts());
                    try {
                        plot3.save("C:\\Users\\tpepe\\Desktop\\charts\\plot_values" + board.getMoveString(c.move), "png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Plot plot2 = Plot.plot(Plot.plotOpts().title("Move plot").height(1080).width(1920).legend(Plot.LegendFormat.TOP)).
                            yAxis("y", Plot.axisOpts().range(-1, 1)).
                            xAxis("x", Plot.axisOpts().range(0, c.getVisits()).format(Plot.AxisFormat.NUMBER_INT))
                            .series("mean", mean, Plot.seriesOpts().line(Plot.Line.DASHED).color(Color.RED))
                            .series("S+", poscusum, Plot.seriesOpts().line(Plot.Line.SOLID).color(Color.BLACK))
                            .series("S-", negcusum, Plot.seriesOpts().line(Plot.Line.SOLID).color(Color.GREEN));
                    try {
                        plot2.save("C:\\Users\\tpepe\\Desktop\\charts\\cusum\\plot_cusum" + board.getMoveString(c.move), "png");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Pack the transpositions
        tt.pack(options.trans_offset);
        this.root = null;
        this.board = null;
        System.gc();
        if (moveCallback != null)
            moveCallback.makeMove(bestMove);
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

    private void cleanDir(File directory) {
        File[] files = directory.listFiles();
        for(File f : files) {
            if(f.isDirectory())
                cleanDir(f);
            else
                f.delete();
        }
    }
}

