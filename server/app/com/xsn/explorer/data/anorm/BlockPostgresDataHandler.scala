package com.xsn.explorer.data.anorm

import javax.inject.Inject

import com.alexitc.playsonify.core.ApplicationResult
import com.xsn.explorer.data.BlockBlockingDataHandler
import com.xsn.explorer.data.anorm.dao.BlockPostgresDAO
import com.xsn.explorer.errors._
import com.xsn.explorer.models.Blockhash
import com.xsn.explorer.models.rpc.Block
import org.scalactic.{One, Or}
import play.api.db.Database

class BlockPostgresDataHandler @Inject() (
    override val database: Database,
    blockPostgresDAO: BlockPostgresDAO)
    extends BlockBlockingDataHandler
    with AnormPostgresDataHandler {

  override def insert(block: Block): ApplicationResult[Block] = {
    val result = withConnection { implicit conn =>
      val maybe = blockPostgresDAO.insert(block)
      Or.from(maybe, One(BlockUnknownError))
    }

    result.badMap { errors =>
      errors.map {
        case PostgresForeignKeyViolationError("blockhash", _) => RepeatedBlockhashError
        case PostgresForeignKeyViolationError("height", _) => RepeatedBlockHeightError
        case e => e
      }
    }
  }

  override def getBy(blockhash: Blockhash): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getBy(blockhash)
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def delete(blockhash: Blockhash): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.delete(blockhash)
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getLatestBlock(): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getLatestBlock
    Or.from(maybe, One(BlockNotFoundError))
  }

  override def getFirstBlock(): ApplicationResult[Block] = database.withConnection { implicit conn =>
    val maybe = blockPostgresDAO.getFirstBlock
    Or.from(maybe, One(BlockNotFoundError))
  }
}
